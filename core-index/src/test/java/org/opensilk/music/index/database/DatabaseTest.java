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
import org.opensilk.music.model.Track;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

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
    static {
        ShadowLog.stream = System.out;
    }

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
        Assertions.assertThat(insId).isEqualTo(lookId);
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

        cv = new ContentValues();
        cv.put(IndexSchema.TrackMeta.TRACK_NAME, "track1");
        cv.put(IndexSchema.TrackMeta.TRACK_KEY, MediaStore.Audio.keyFor("track1"));
        cv.put(IndexSchema.TrackMeta.ARTIST_ID, artistId);
        cv.put(IndexSchema.TrackMeta.ALBUM_ID, albumId);
        long trackId = mDb.insert(IndexSchema.TrackMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        Uri containerUri = Uri.parse("content://sample/foo/bar");
        long containerId = mDb.insertContainer(containerUri, Uri.parse("content://sample/foo"));

        long[] resIds = null;
        for (int ii=0; ii<10; ii++) {
            Track.Res res = Track.Res.builder()
                    .setUri(Uri.parse("content://sample/track1/res/" + ii))
                    .build();
            resIds = ArrayUtils.add(resIds, mDb.insertTrackRes(res, trackId, containerId));
        }

        StringBuilder where = new StringBuilder();
        where.append(IndexSchema.TrackResMeta.RES_ID).append(" IN (").append(resIds[0]);
        for (int ii=1; ii<resIds.length; ii++) {
            where.append(",").append(resIds[ii]);
        }
        where.append(")");

        //make sure whe actually have the entries were trying to delete

        Cursor c = mDb.query(IndexSchema.TrackResMeta.TABLE,
                new String[] {IndexSchema.TrackResMeta.RES_ID}, where.toString(), null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(10);

        c = mDb.query(IndexSchema.TrackMeta.TABLE, new String[] {IndexSchema.TrackMeta.TRACK_ID},
                IndexSchema.TrackMeta.TRACK_ID + "=" + trackId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(1);

        c = mDb.query(IndexSchema.AlbumMeta.TABLE, new String[]{IndexSchema.AlbumMeta.ALBUM_ID},
                IndexSchema.AlbumMeta.ALBUM_ID + "=" + albumId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(1);

        c = mDb.query(IndexSchema.ArtistMeta.TABLE, new String[]{IndexSchema.ArtistMeta.ARTIST_ID},
                IndexSchema.ArtistMeta.ARTIST_ID + "=" + artistId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(1);

        //Will cascade all the track.res which will trigger deletion of track,album,and artist
        mDb.removeContainer(containerUri);

        //track should have been deleted
        c = mDb.query(IndexSchema.TrackMeta.TABLE, new String[] {IndexSchema.TrackMeta.TRACK_ID},
                IndexSchema.TrackMeta.TRACK_ID + "=" + trackId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(0);
        //album should have been deleted
        c = mDb.query(IndexSchema.AlbumMeta.TABLE, new String[]{IndexSchema.AlbumMeta.ALBUM_ID},
                IndexSchema.AlbumMeta.ALBUM_ID + "=" + albumId, null, null, null, null);
        Assertions.assertThat(c).isNotNull();
        Assertions.assertThat(c.getCount()).isEqualTo(0);
        //artist should have been deleted
        c = mDb.query(IndexSchema.ArtistMeta.TABLE, new String[]{IndexSchema.ArtistMeta.ARTIST_ID},
                IndexSchema.ArtistMeta.ARTIST_ID + "=" + artistId, null, null, null, null);
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

        cv = new ContentValues();
        cv.put(IndexSchema.TrackMeta.TRACK_NAME, "track2");
        cv.put(IndexSchema.TrackMeta.TRACK_KEY, MediaStore.Audio.keyFor("track2"));
//        cv.put(IndexSchema.TrackMeta.TRACK_NUMBER, 2);
        cv.put(IndexSchema.TrackMeta.ARTIST_ID, artistId);
        cv.put(IndexSchema.TrackMeta.ALBUM_ID, albumId);

        long trackId = mDb.insert(IndexSchema.TrackMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);
        Assertions.assertThat(trackId).isNotEqualTo(-1);
        long trackId2 = mDb.insert(IndexSchema.TrackMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        Assertions.assertThat(trackId2).isNotEqualTo(-1);
        Assertions.assertThat(trackId2).isEqualTo(trackId);
    }

    @Test
    public void testHasTrack() {
        ContentValues cv = new ContentValues();
        cv.put(IndexSchema.AlbumMeta.ALBUM_NAME, "album3");
        cv.put(IndexSchema.AlbumMeta.ALBUM_KEY, MediaStore.Audio.keyFor("album3"));
        long albumId = mDb.insert(IndexSchema.AlbumMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv = new ContentValues();
        cv.put(IndexSchema.ArtistMeta.ARTIST_NAME, "artist3");
        cv.put(IndexSchema.ArtistMeta.ARTIST_KEY, MediaStore.Audio.keyFor("artist3"));
        long artistId = mDb.insert(IndexSchema.ArtistMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv = new ContentValues();
        cv.put(IndexSchema.TrackMeta.TRACK_NAME, "track3");
        cv.put(IndexSchema.TrackMeta.TRACK_KEY, MediaStore.Audio.keyFor("track3"));
        cv.put(IndexSchema.TrackMeta.TRACK_NUMBER, 1);
        cv.put(IndexSchema.TrackMeta.ARTIST_ID, artistId);
        cv.put(IndexSchema.TrackMeta.ALBUM_ID, albumId);

        long trackId = mDb.insert(IndexSchema.TrackMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        cv.put(IndexSchema.TrackMeta.TRACK_NUMBER, 2);
        long trackId2 = mDb.insert(IndexSchema.TrackMeta.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);

        Uri containerUri = Uri.parse("content://sample/foo2/bar");
        long containerId = mDb.insertContainer(containerUri, Uri.parse("content://sample/foo2"));

        long[] resIds = null;
        for (int ii=0; ii<10; ii++) {
            Track.Res res = Track.Res.builder()
                    .setUri(Uri.parse("content://sample/track3/res/" + ii))
                    .build();
            resIds = ArrayUtils.add(resIds, mDb.insertTrackRes(res, trackId, containerId));
        }

        Track.Builder tob = Track.builder()
                .setUri(Uri.parse("content://sample/track3"))
                .setName("track3")
                .setTrackNumber(1)
                .addRes(Track.Res.builder()
                        .setUri(Uri.parse("content://sample/track3/res/1"))
                        .build())
                ;

        long trackId3 = mDb.hasTrack(tob.build(), artistId, albumId);

        Assertions.assertThat(trackId3).isEqualTo(trackId);

        long trackId4 = mDb.hasTrack(tob.setTrackNumber(2).build(), artistId, albumId);

        Assertions.assertThat(trackId4).isEqualTo(trackId2);
    }


}
