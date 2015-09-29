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

import android.net.Uri;
import android.os.Build;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.IndexTestComponent;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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
    public void testDbIsCreated() {
        ((IndexDatabaseImpl) mDb).helper.getWritableDatabase();
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
        Uri containerUri = Uri.parse("content://sample/foo/bar");
        Uri containerParentUri = Uri.parse("content://sample/foo");
        long containerId = mDb.insertContainer(containerUri, containerParentUri);

        long[] trackIds = null;
        for (int ii=0; ii<10; ii++) {
            Track track = Track.builder()
                    .setUri(Uri.parse("content://sample/track" + ii))
                    .setName("track" + ii)
                    .setParentUri(containerUri)
                    .addRes(Track.Res.builder().setUri(Uri.parse("content://sample/res"+ii)).build())
                    .build();
            Metadata meta = Metadata.builder()
                    .putString(Metadata.KEY_TRACK_NAME, "metatrack"+ii)
                    .putString(Metadata.KEY_ALBUM_NAME, "album"+ii%2)
                    .putString(Metadata.KEY_ARTIST_NAME, "artist" + ii % 2)
                    .putString(Metadata.KEY_ALBUM_ARTIST_NAME, "artist"+ii%2)
                    .build();
            long trackId = mDb.insertTrack(track, meta);
            Assertions.assertThat(trackId).isGreaterThan(0);
            trackIds = ArrayUtils.add(trackIds, trackId);
        }

        //make sure we have the number of items we expect
        Assertions.assertThat(mDb.getArtists(null).size()).isEqualTo(2);
        Assertions.assertThat(mDb.getAlbums(null).size()).isEqualTo(2);
        Assertions.assertThat(mDb.getTracks(null).size()).isEqualTo(10);

        //removee the container
        long numremoved = mDb.removeContainer(containerUri);
        Assertions.assertThat(numremoved).isEqualTo(1);

        //check that triggers cleared out tables
        Assertions.assertThat(mDb.getArtists(null).size()).isEqualTo(0);
        Assertions.assertThat(mDb.getAlbums(null).size()).isEqualTo(0);
        Assertions.assertThat(mDb.getTracks(null).size()).isEqualTo(0);
    }


}
