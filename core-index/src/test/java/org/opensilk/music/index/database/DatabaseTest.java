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
import org.opensilk.music.model.sort.TrackSortOrder;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.opensilk.music.index.database.TestData.*;

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
        long insId = mDb.insertContainer(URI_SFB, URI_SFB_PARENT);
        long lookId = mDb.hasContainer(URI_SFB);
        Assertions.assertThat(lookId).isEqualTo(insId);
        for (Uri child : URI_SFB_CHILDREN_0_10) {
            long iid = mDb.insertContainer(child, URI_SFB);
            long lid = mDb.hasContainer(child);
            Assertions.assertThat(iid).isEqualTo(lid);
        }
        int cnt = mDb.removeContainer(URI_SFB);
        Assertions.assertThat(cnt).isEqualTo(11);
    }

    @Test
    public void testAddRemoveContainersMoreThan50Deletions() {
        long insId = mDb.insertContainer(URI_SFB, URI_SFB_PARENT);
        long lookId = mDb.hasContainer(URI_SFB);
        Assertions.assertThat(lookId).isEqualTo(insId);
        for (Uri child : URI_SFB_CHILDREN_0_125) {
            long iid = mDb.insertContainer(child, URI_SFB);
            long lid = mDb.hasContainer(child);
            Assertions.assertThat(iid).isEqualTo(lid);
        }
        int cnt = mDb.removeContainer(URI_SFB);
        Assertions.assertThat(cnt).isEqualTo(126);
    }

    @Test
    public void testDoubleContainerInsert() {
        long insId = mDb.insertContainer(URI_SFB, URI_SFB_PARENT);
        long insId2 =mDb.insertContainer(URI_SFB, URI_SFB_PARENT);
        Assertions.assertThat(insId).isEqualTo(insId2);
    }

    @Test
    public void testRecusiveContainerRemoval() {
        Uri uri = Uri.parse("content://foo/1");
        Uri uri2 = Uri.parse("content://foo/2");
        Uri uri3 = Uri.parse("content://foo/3");
        Uri uri4 = Uri.parse("content://foo/4");
        mDb.insertContainer(uri2, uri);
        mDb.insertContainer(uri3, uri2);
        mDb.insertContainer(uri4, uri3);
        int cnt = mDb.removeContainer(uri2);
        Assertions.assertThat(cnt).isEqualTo(3);
    }

    @Test
    public void testCleanupMetaTriggers() {
        long containerId = mDb.insertContainer(URI_SFB, URI_SFB_PARENT);
        long[] trackIds = null;
        for (int ii=0; ii<10; ii++) {
            long trackId = mDb.insertTrack(TRACK_SFB_0_10.get(ii), METADATA_TRACK_SFB_0_10.get(ii));
            Assertions.assertThat(trackId).isGreaterThan(0);
            trackIds = ArrayUtils.add(trackIds, trackId);
        }
        //make sure we have the number of items we expect
        Assertions.assertThat(mDb.getArtists(null, null).size()).isEqualTo(2);
        Assertions.assertThat(mDb.getAlbums(null, null).size()).isEqualTo(2);
        Assertions.assertThat(mDb.getTracks(null, null).size()).isEqualTo(10);

        //removee the container
        long numremoved = mDb.removeContainer(URI_SFB);
        Assertions.assertThat(numremoved).isEqualTo(1);

        //check that triggers cleared out tables
        Assertions.assertThat(mDb.getArtists(null, null).size()).isEqualTo(0);
        Assertions.assertThat(mDb.getAlbums(null, null).size()).isEqualTo(0);
        Assertions.assertThat(mDb.getTracks(null, null).size()).isEqualTo(0);
    }

    @Test
    public void testPlaylistOperations() {
        Uri containerUri = Uri.parse("content://sample2/foo/bar");
        Uri containerParentUri = Uri.parse("content://sample2/foo");
        mDb.insertContainer(containerUri, containerParentUri);

        long[] trackIds = null;
        for (int ii=0; ii<10; ii++) {
            Track track = Track.builder()
                    .setUri(Uri.parse("content://sample2/track" + ii))
                    .setName("track" + ii)
                    .setParentUri(containerUri)
                    .addRes(Track.Res.builder().setUri(Uri.parse("content://sample2/res"+ii)).build())
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

        long plid = mDb.insertPlaylist("Pl1");
        Assertions.assertThat(plid).isGreaterThan(0);

        //test initial insert
        List<Uri> list = new ArrayList<>(5);
        for (int ii=0; ii<5; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }
        Collections.shuffle(list);
        long num = mDb.addToPlaylist(String.valueOf(plid), list);
        Assertions.assertThat(num).isEqualTo(list.size());
        List<Track> pltracks = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        Assertions.assertThat(pltracks.size()).isEqualTo(list.size());
        Iterator<Uri> uii = list.iterator();
        Iterator<Track> tii = pltracks.iterator();
        while (uii.hasNext() && tii.hasNext()) {
            Assertions.assertThat(uii.next()).isEqualTo(tii.next().getUri());
        }

        mDb.getTracks(null, null);

        List<Uri> list2 = new ArrayList<>(4);
        for (int ii= 7; ii< 10; ii++) {
            list2.add(Uri.parse("content://sample2/track" + ii));
        }

        //test additonal insert
        Collections.shuffle(list2);
        long num2 = mDb.addToPlaylist(String.valueOf(plid), list2);
        Assertions.assertThat(num2).isEqualTo(list2.size());
        list.addAll(list2);
        pltracks = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        Assertions.assertThat(pltracks.size()).isEqualTo(list.size());
        uii = list.iterator();
        tii = pltracks.iterator();
        while (uii.hasNext() && tii.hasNext()) {
            Assertions.assertThat(uii.next()).isEqualTo(tii.next().getUri());
        }

        //check the order
        tii = pltracks.iterator();
        int zz=0;
        while (tii.hasNext()) {
            Assertions.assertThat(tii.next().getTrackNumber()).isEqualTo(zz++);
        }

        //
        // move from < to

        plid = mDb.insertPlaylist("Pl2");
        Assertions.assertThat(plid).isGreaterThan(0);

        list = new ArrayList<>(8);
        for (int ii=0; ii<8; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }

        mDb.addToPlaylist(String.valueOf(plid), list);
        List<Track> pltracks4 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);

        mDb.movePlaylistEntry(String.valueOf(plid), 3, 6);
        List<Track> pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        Iterator<Track> tii3 = pltracks4.iterator();
        Iterator<Track> tii4 = pltracks5.iterator();
        while (tii3.hasNext() && tii4.hasNext()) {
            Track next = tii3.next();
            Track next2 = tii4.next();
            System.out.println("" + next.getTrackNumber() + "  " + next.getUri() +
            "  " + next2.getTrackNumber() + "  " + next2.getUri());
        }
        Assertions.assertThat(pltracks5.get(0).getUri()).isEqualTo(pltracks4.get(0).getUri());
        Assertions.assertThat(pltracks5.get(1).getUri()).isEqualTo(pltracks4.get(1).getUri());
        Assertions.assertThat(pltracks5.get(2).getUri()).isEqualTo(pltracks4.get(2).getUri());
        Assertions.assertThat(pltracks5.get(3).getUri()).isEqualTo(pltracks4.get(4).getUri());
        Assertions.assertThat(pltracks5.get(4).getUri()).isEqualTo(pltracks4.get(5).getUri());
        Assertions.assertThat(pltracks5.get(5).getUri()).isEqualTo(pltracks4.get(6).getUri());
        Assertions.assertThat(pltracks5.get(6).getUri()).isEqualTo(pltracks4.get(3).getUri());
        Assertions.assertThat(pltracks5.get(7).getUri()).isEqualTo(pltracks4.get(7).getUri());

        //
        // move from > to

        plid = mDb.insertPlaylist("Pl3");
        Assertions.assertThat(plid).isGreaterThan(0);

        list = new ArrayList<>(8);
        for (int ii=0; ii<8; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }

        mDb.addToPlaylist(String.valueOf(plid), list);
        pltracks4 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);

        mDb.movePlaylistEntry(String.valueOf(plid), 5, 2);
        pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        tii3 = pltracks4.iterator();
        tii4 = pltracks5.iterator();
        while (tii3.hasNext() && tii4.hasNext()) {
            Track next = tii3.next();
            Track next2 = tii4.next();
            System.out.println("" + next.getTrackNumber() + "  " + next.getUri() +
                    "  " + next2.getTrackNumber() + "  " + next2.getUri());
        }
        Assertions.assertThat(pltracks5.get(0).getUri()).isEqualTo(pltracks4.get(0).getUri());
        Assertions.assertThat(pltracks5.get(1).getUri()).isEqualTo(pltracks4.get(1).getUri());
        Assertions.assertThat(pltracks5.get(2).getUri()).isEqualTo(pltracks4.get(5).getUri());
        Assertions.assertThat(pltracks5.get(3).getUri()).isEqualTo(pltracks4.get(2).getUri());
        Assertions.assertThat(pltracks5.get(4).getUri()).isEqualTo(pltracks4.get(3).getUri());
        Assertions.assertThat(pltracks5.get(5).getUri()).isEqualTo(pltracks4.get(4).getUri());
        Assertions.assertThat(pltracks5.get(6).getUri()).isEqualTo(pltracks4.get(6).getUri());
        Assertions.assertThat(pltracks5.get(7).getUri()).isEqualTo(pltracks4.get(7).getUri());

        //
        //removing

        plid = mDb.insertPlaylist("Pl4");
        Assertions.assertThat(plid).isGreaterThan(0);

        list = new ArrayList<>(8);
        for (int ii=0; ii<8; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }

        mDb.addToPlaylist(String.valueOf(plid), list);
        pltracks4 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);

        mDb.removeFromPlaylist(String.valueOf(plid), Uri.parse("content://sample2/track4"));
        pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        tii3 = pltracks4.iterator();
        tii4 = pltracks5.iterator();
        while (tii3.hasNext() && tii4.hasNext()) {
            Track next = tii3.next();
            Track next2 = tii4.next();
            System.out.println("" + next.getTrackNumber() + "  " + next.getUri() +
                    "  " + next2.getTrackNumber() + "  " + next2.getUri());
        }

        Assertions.assertThat(pltracks5.size()).isEqualTo(7);

        //check the order
        tii = pltracks5.iterator();
        zz=0;
        while (tii.hasNext()) {
            Assertions.assertThat(tii.next().getTrackNumber()).isEqualTo(zz++);
        }

        Assertions.assertThat(pltracks5.get(0).getUri()).isEqualTo(pltracks4.get(0).getUri());
        Assertions.assertThat(pltracks5.get(1).getUri()).isEqualTo(pltracks4.get(1).getUri());
        Assertions.assertThat(pltracks5.get(2).getUri()).isEqualTo(pltracks4.get(2).getUri());
        Assertions.assertThat(pltracks5.get(3).getUri()).isEqualTo(pltracks4.get(3).getUri());
        Assertions.assertThat(pltracks5.get(4).getUri()).isEqualTo(pltracks4.get(5).getUri());
        Assertions.assertThat(pltracks5.get(5).getUri()).isEqualTo(pltracks4.get(6).getUri());
        Assertions.assertThat(pltracks5.get(6).getUri()).isEqualTo(pltracks4.get(7).getUri());
        //Assertions.assertThat(pltracks5.get(7).getUri()).isEqualTo(pltracks4.get(7).getUri());

        //
        //removing with pos

        plid = mDb.insertPlaylist("Pl5");
        Assertions.assertThat(plid).isGreaterThan(0);

        list = new ArrayList<>(8);
        for (int ii=0; ii<8; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }

        mDb.addToPlaylist(String.valueOf(plid), list);
        pltracks4 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);

        mDb.removeFromPlaylist(String.valueOf(plid), 4);
        pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        tii3 = pltracks4.iterator();
        tii4 = pltracks5.iterator();
        while (tii3.hasNext() && tii4.hasNext()) {
            Track next = tii3.next();
            Track next2 = tii4.next();
            System.out.println("" + next.getTrackNumber() + "  " + next.getUri() +
                    "  " + next2.getTrackNumber() + "  " + next2.getUri());
        }

        Assertions.assertThat(pltracks5.size()).isEqualTo(7);

        //check the order
        tii = pltracks5.iterator();
        zz=0;
        while (tii.hasNext()) {
            Assertions.assertThat(tii.next().getTrackNumber()).isEqualTo(zz++);
        }

        Assertions.assertThat(pltracks5.get(0).getUri()).isEqualTo(pltracks4.get(0).getUri());
        Assertions.assertThat(pltracks5.get(1).getUri()).isEqualTo(pltracks4.get(1).getUri());
        Assertions.assertThat(pltracks5.get(2).getUri()).isEqualTo(pltracks4.get(2).getUri());
        Assertions.assertThat(pltracks5.get(3).getUri()).isEqualTo(pltracks4.get(3).getUri());
        Assertions.assertThat(pltracks5.get(4).getUri()).isEqualTo(pltracks4.get(5).getUri());
        Assertions.assertThat(pltracks5.get(5).getUri()).isEqualTo(pltracks4.get(6).getUri());
        Assertions.assertThat(pltracks5.get(6).getUri()).isEqualTo(pltracks4.get(7).getUri());

        //
        //update Pl5

        list = new ArrayList<>(8);
        for (int ii=0; ii<8; ii++) {
            list.add(Uri.parse("content://sample2/track" + ii));
        }

        Collections.shuffle(list);

        mDb.updatePlaylist(String.valueOf(plid), list);
        pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        tii3 = pltracks4.iterator();
        tii4 = pltracks5.iterator();
        while (tii3.hasNext() && tii4.hasNext()) {
            Track next = tii3.next();
            Track next2 = tii4.next();
            System.out.println("" + next.getTrackNumber() + "  " + next.getUri() +
                    "  " + next2.getTrackNumber() + "  " + next2.getUri());
        }

        Assertions.assertThat(pltracks5.size()).isEqualTo(8);

        //check the order
        tii = pltracks5.iterator();
        zz=0;
        while (tii.hasNext()) {
            Assertions.assertThat(tii.next().getTrackNumber()).isEqualTo(zz++);
        }

        Assertions.assertThat(pltracks5.get(0).getUri()).isEqualTo(list.get(0));
        Assertions.assertThat(pltracks5.get(1).getUri()).isEqualTo(list.get(1));
        Assertions.assertThat(pltracks5.get(2).getUri()).isEqualTo(list.get(2));
        Assertions.assertThat(pltracks5.get(3).getUri()).isEqualTo(list.get(3));
        Assertions.assertThat(pltracks5.get(4).getUri()).isEqualTo(list.get(4));
        Assertions.assertThat(pltracks5.get(5).getUri()).isEqualTo(list.get(5));
        Assertions.assertThat(pltracks5.get(6).getUri()).isEqualTo(list.get(6));
        Assertions.assertThat(pltracks5.get(7).getUri()).isEqualTo(list.get(7));

        //update remove all

        list.clear();
        mDb.updatePlaylist(String.valueOf(plid), list);
        pltracks5 = mDb.getPlaylistTracks(String.valueOf(plid), TrackSortOrder.PLAYORDER);
        Assertions.assertThat(pltracks5.size()).isEqualTo(0);

    }

    @Test
    public void testSaveGetQueue() {
        mDb.saveQueue(URI_SFB_CHILDREN_0_10);
        List<Uri> queue = mDb.getLastQueue();
        Assertions.assertThat(queue).isEqualTo(URI_SFB_CHILDREN_0_10);
    }

    @Test
    public void testSaveGetLastQueuePos() {
        mDb.saveQueuePosition(4);
        Assertions.assertThat(mDb.getLastQueuePosition()).isEqualTo(4);
    }

    @Test
    public void testSaveGetLastQueueShuffleMode() {
        mDb.saveQueueShuffleMode(2);
        Assertions.assertThat(mDb.getLastQueueShuffleMode()).isEqualTo(2);
    }

    @Test
    public void testSaveGetLastQueueRepeatMode() {
        mDb.saveQueueRepeatMode(1);
        Assertions.assertThat(mDb.getLastQueueRepeatMode()).isEqualTo(1);
    }

    @Test
    public void testSaveGetlastSeekPos() {
        mDb.saveLastSeekPosition(300);
        Assertions.assertThat(mDb.getLastSeekPosition()).isEqualTo(300);
    }

    @Test
    public void testSaveGetBroadcastMeta() {
        mDb.setBroadcastMeta(true);
        Assertions.assertThat(mDb.getBroadcastMeta()).isTrue();
    }

    @Test
    public void testCoalesce() {
        Assertions.assertThat(IndexDatabaseImpl.coalesce("foo", null)).isEqualTo("foo");
        Assertions.assertThat(IndexDatabaseImpl.coalesce("", "bar")).isEqualTo("bar");
        Assertions.assertThat(IndexDatabaseImpl.coalesceOrUnknown("", null)).isEqualTo(IndexSchema.UNKNOWN_STRING);
        Assertions.assertThat(IndexDatabaseImpl.coalesceOrUnknown(null, "foo")).isEqualTo("foo");
    }

}
