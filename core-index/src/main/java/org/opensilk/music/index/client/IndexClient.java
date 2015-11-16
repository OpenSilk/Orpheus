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

package org.opensilk.music.index.client;

import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import org.opensilk.music.model.Container;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.List;

import rx.Observable;

/**
 * Created by drew on 9/17/15.
 */
public interface IndexClient {

    /*
     * Index methods
     */

    boolean isIndexed(Container container);
    boolean add(Container container);
    boolean remove(Container container);
    void rescan();


    /*
     * Android auto entry points
     */
    List<MediaDescriptionCompat> getAutoRoots();


    /*
     * Playback settings
     */

    List<Uri> getLastQueue();
    int getLastQueuePosition();
    int getLastQueueShuffleMode();
    int getLastQueueRepeatMode();

    void saveQueue(List<Uri> queue);
    void saveQueuePosition(int pos);
    void saveQueueShuffleMode(int mode);
    void saveQueueRepeatMode(int mode);

    long getLastSeekPosition();
    void saveLastSeekPosition(long pos);

    boolean broadcastMeta();
    void setBroadcastMeta(boolean broadcastMeta);

    /*
     * Queue / Playback helper
     */

    Observable<List<MediaDescriptionCompat>> getDescriptions(List<Uri> queue);
    Observable<Track> getTrack(Uri uri);
    MediaMetadataCompat convertToMediaMetadata(Track track);

    /*
     * Playlist helpers
     */

    Uri createPlaylist(String name);
    int addToPlaylist(Uri playlist, List<Uri> tracks);
    int movePlaylistEntry(Uri playlist, int from, int to);
    int removeFromPlaylist(Uri playlist, int position);
    int updatePlaylist(Uri playlist, List<Uri> uris);
    boolean removePlaylists(List<Uri> playlists);
    Playlist getPlaylist(Uri playlist);

    /*
     * setup/destroy
     */

    void startBatch();
    void endBatch();

}
