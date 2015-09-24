/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.index.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.IndexTestComponent;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.library.sort.ArtistSortOrder;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.UUID;

/**
 * Created by drew on 9/20/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class DatabaseTest {

    IndexDatabase mDb;

    @Before
    public void setup() {
        IndexTestComponent cmpt = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
        mDb = cmpt.indexDatabase();
    }

    @Test
    public void testAddRemoveContainers() {
        Uri uri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo").appendPath("bar").build();
        Uri parentUri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo").build();
        long insId = mDb.insertContainer(uri, parentUri);
        long lookId = mDb.hasContainer(uri);
        Assertions.assertThat(lookId).isEqualTo(insId);
        for (int ii=0; ii<10; ii++) {
            Uri child = new Uri.Builder().scheme("content").authority("sample")
                    .appendPath("foo").appendPath("bar").appendPath(String.valueOf(ii)).build();
            long iid = mDb.insertContainer(child, uri);
            long lid = mDb.hasContainer(child);
            Assertions.assertThat(iid).isEqualTo(lid);
        }
        int cnt = mDb.removeContainer(uri);
        Assertions.assertThat(cnt).isEqualTo(11);
    }

    @Test
    public void testDoubleContainerInsert() {
        Uri uri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo2").appendPath("bar").build();
        Uri parentUri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo2").build();
        long insId = mDb.insertContainer(uri, parentUri);
        long insId2 =mDb.insertContainer(uri, parentUri);
        Assertions.assertThat(insId).isEqualTo(insId2);
    }

    @Test
    public void testCleanupMetaTriggers() {
        ContentValues cv = new ContentValues();
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, "album1");
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, MediaStore.Audio.keyFor("album1"));
        cv.put(IndexSchema.AlbumMeta.ALBUM_MBID, UUID.randomUUID().toString());
        long albumId = mDb.insert(IndexSchema.AlbumMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv = new ContentValues();
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, "artist1");
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, MediaStore.Audio.keyFor("artist1"));
        cv.put(IndexSchema.ArtistMeta.ARTIST_MBID, UUID.randomUUID().toString());
        long artistId = mDb.insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        Uri containerUri = Uri.parse("content://sample/foo/bar");
        long containerId = mDb.insertContainer(containerUri, Uri.parse("content://sample/foo"));

        long[] resIds = null;
        for (int ii=0; ii<10; ii++) {
            Metadata.Builder bob = Metadata.builder()
                    .putUri(Metadata.KEY_TRACK_URI, Uri.parse("content://sample/track/" + ii))
                    .putUri(Metadata.KEY_PARENT_URI, containerUri)
                    .putString(Metadata.KEY_TRACK_NAME, "track" + ii)
                    ;
            resIds = ArrayUtils.add(resIds, mDb.insertTrackResource(bob.build(), albumId, artistId, containerId));
        }

        StringBuilder where = new StringBuilder();
        where.append(IndexSchema.TrackResMeta._ID).append(" IN (").append(resIds[0]);
        for (int ii=1; ii<resIds.length; ii++) {
            where.append(",").append(resIds[ii]);
        }
        where.append(")");

        //make sure whe actually have the entries were trying to delete

        Cursor c = mDb.query(IndexSchema.TrackResMeta.TABLE,
                new String[] {IndexSchema.TrackResMeta._ID}, where.toString(), null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(10);

        c = mDb.query(IndexSchema.AlbumMeta.TABLE, new String[]{IndexSchema.AlbumMeta._ID},
                IndexSchema.AlbumMeta._ID + "=" + albumId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(1);

        c = mDb.query(IndexSchema.ArtistMeta.TABLE, new String[]{IndexSchema.ArtistMeta._ID},
                IndexSchema.ArtistMeta._ID + "=" + artistId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(1);

        //Will cascade all the track.res which will trigger deletion of track,album,and artist
        mDb.removeContainer(containerUri);

        //track should have been deleted
        c = mDb.query(IndexSchema.TrackResMeta.TABLE, new String[] {IndexSchema.TrackResMeta._ID},
                null, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(0);
        //album should have been deleted
        c = mDb.query(IndexSchema.AlbumMeta.TABLE, new String[]{IndexSchema.AlbumMeta._ID},
                IndexSchema.AlbumMeta._ID + "=" + albumId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(0);
        //artist should have been deleted
        c = mDb.query(IndexSchema.ArtistMeta.TABLE, new String[]{IndexSchema.ArtistMeta._ID},
                IndexSchema.ArtistMeta._ID + "=" + artistId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(0);

    }

    @Test
    public void testDoubleTrackInsert() {
        ContentValues cv = new ContentValues();
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, "album2");
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, MediaStore.Audio.keyFor("album2"));
        long albumId = mDb.insert(IndexSchema.AlbumMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv = new ContentValues();
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, "artist2");
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, MediaStore.Audio.keyFor("artist2"));
        long artistId = mDb.insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        Uri containerUri = Uri.parse("content://sample/foo/bar");
        long containerId = mDb.insertContainer(containerUri, Uri.parse("content://sample/foo"));

        Metadata.Builder bob = Metadata.builder()
                .putUri(Metadata.KEY_TRACK_URI, Uri.parse("content://sample/track/1"))
                .putString(Metadata.KEY_TRACK_NAME, "track1")
                ;

        long trackId = mDb.insertTrackResource(bob.build(), albumId, artistId, containerId);
        Assertions.assertThat(trackId).isNotEqualTo(-1);
        long trackId2 = mDb.insertTrackResource(bob.build(), albumId, artistId, containerId);
        Assertions.assertThat(trackId2).isEqualTo(trackId);
    }

    @Test
    public void testInfoViewsWithDuplicateTracks() {

        ContentValues cv = new ContentValues();
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, "artist2");
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, MediaStore.Audio.keyFor("artist2"));
        long artistId = mDb.insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv = new ContentValues();
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, "album2");
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, MediaStore.Audio.keyFor("album2"));
        cv.put(IndexSchema.AlbumMeta.ALBUM_ARTIST_ID, artistId);
        long albumId = mDb.insert(IndexSchema.AlbumMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        for (int ii=0; ii<5; ii++) {
            String authority = String.valueOf(ii);
            Uri containerUri = Uri.parse("content://" + authority + "/foo/bar");
            long containerId = mDb.insertContainer(containerUri, Uri.parse("content://" + authority + "/foo"));
            Assertions.assertThat(containerId).isNotEqualTo(-1);

            Metadata.Builder bob = Metadata.builder()
                    .putUri(Metadata.KEY_TRACK_URI, Uri.parse("content://" + authority + "/track/1"))
                            .putString(Metadata.KEY_TRACK_NAME, "track1");
            long trackId = mDb.insertTrackResource(bob.build(), albumId, artistId, containerId);
            Assertions.assertThat(trackId).isNotEqualTo(-1);
        }

        List<Track> tracks = mDb.getTracks(TrackSortOrder.A_Z, true);
        Assertions.assertThat(tracks.size()).isEqualTo(1);
        Assertions.assertThat(tracks.get(0).getResources().size()).isEqualTo(5);

        List<Artist> artist = mDb.getArtists(ArtistSortOrder.A_Z);
        Assertions.assertThat(artist.size()).isEqualTo(1);
        Assertions.assertThat(artist.get(0).getTrackCount()).isEqualTo(1);

        List<Album> albums = mDb.getAlbums(AlbumSortOrder.A_Z);
        Assertions.assertThat(albums.size()).isEqualTo(1);
        Assertions.assertThat(albums.get(0).getTrackCount()).isEqualTo(1);

    }


}
