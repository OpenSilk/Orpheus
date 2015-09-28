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

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;

import org.opensilk.music.model.Container;
import org.opensilk.music.model.Track;

import java.util.List;

import rx.Observable;

/**
 * Created by drew on 9/17/15.
 */
public interface IndexClient {
    /**
     * @param uri
     * @return True if uri or any ancestor is indexed
     */
    boolean isIndexed(Container container);

    /**
     *
     * @param uri
     * @return True if succeeded, false on error or if {@link #isIndexed(Uri)} returns true;
     */
    boolean add(Container container);

    /**
     *
     * @param uri
     * @return True if success, false on error or if {@link #isIndexed(Uri)} returns false;
     */
    boolean remove(Container container);

    MediaBrowserService.BrowserRoot browserGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints);
    void browserLoadChildren(@NonNull String parentId, @NonNull MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result);


    List<Uri> getLastQueue();
    int getLastQueuePosition();
    int getLastQueueShuffleMode();
    int getLastQueueRepeatMode();

    void saveQueue(List<Uri> queue);
    void saveQueuePosition(int pos);
    void saveQueueShuffleMode(int mode);
    void saveQueueRepeatMode(int mode);

    Observable<List<MediaDescriptionCompat>> getDescriptions(List<Uri> queue);

    long getLastSeekPosition();
    void saveLastSeekPosition(long pos);

    Observable<Track> getTrack(Uri uri);
    Observable<List<Track>> getTracks(Uri uri, String sortOrder);
    Observable<List<Uri>> getTrackUris(Uri uri, String sordOrder);
    MediaMetadata convertToMediaMetadata(Track track);
}
