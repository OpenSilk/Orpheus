/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.library.mediastore.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.Track;

import javax.inject.Inject;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateDataUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getLongOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

/**
 * Created by drew on 2/21/14.
 */
public class TracksLoader extends RxCursorLoader<Track> {

    @Inject
    public TracksLoader(@ForApplication Context context) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_MEDIA);
        setProjection(Projections.LOCAL_SONG);
        setSelection(Selections.LOCAL_SONG);
        setSelectionArgs(SelectionArgs.LOCAL_SONG);
        // need set sortorder
    }

    @Override
    protected Track makeFromCursor(Cursor c) throws Exception {
        // Copy the song Id
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        // Copy the song name
        final String songName = getStringOrNull(c, MediaStore.Audio.AudioColumns.TITLE);
        // Copy the artist name
        final String artist = getStringOrNull(c, MediaStore.Audio.AudioColumns.ARTIST);
        // Copy the album name
        final String album = getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM);
        // Copy the album id
        final String albumId = getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM_ID);
        // find the album artist
        final String albumArtist = getStringOrNull(c, "album_artist");
        // Copy the duration
        final long duration = getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION);
        // Make the duration label
        final int seconds = (int) (duration > 0 ? (duration / 1000) : 0);
        // get data uri
        final Uri dataUri = generateDataUri(id);
        // generate artwork uri
        final Uri artworkUri = albumId != null ? generateArtworkUri(albumId) : null;
        // mime
        final String mimeType = getStringOrNull(c, MediaStore.Audio.AudioColumns.MIME_TYPE);
        return Track.builder()
                .setIdentity(id)
                .setName(songName)
                .setArtistName(artist)
                .setAlbumName(album)
                .setAlbumIdentity(albumId)
                .setAlbumArtistName(albumArtist)
                .setDuration(seconds)
                .setDataUri(dataUri)
                .setArtworkUri(artworkUri)
                .setMimeType(mimeType)
                .build();
    }

}
