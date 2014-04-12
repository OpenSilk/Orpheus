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

package org.opensilk.music.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.utils.Lists;

import java.util.List;

/**
 * Created by drew on 4/12/14.
 */
public class PlaylistCursorLoader extends CursorLoader {

    public PlaylistCursorLoader(Context context) {
        super(context);
        setUri(MusicProvider.PLAYLIST_URI);
        // Our content provider doesnt read any of these values
        setProjection(null);
        setSelection(null);
        setSelectionArgs(null);
        setSortOrder(null);
    }

    /**
     * Creates the {@link android.database.Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link android.database.Cursor} used to run the playlist query.
     */
    public static Cursor makePlaylistCursor(final Context context) {
        return context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] {
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        MediaStore.Audio.PlaylistsColumns.NAME
                }, null, null, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
    }

    /**
     * Return list of user playlists, NOTE does not get song count
     * @param context
     * @return
     */
    public static List<Playlist> getUserPlaylists(final Context context) {
        List<Playlist> usrPlaylists = Lists.newArrayList();
        // Create the Cursor
        Cursor c = makePlaylistCursor(context);
        // Gather the data
        if (c != null && c.moveToFirst()) {
            do {
                // Copy the playlist id
                final long id = c.getLong(0);

                // Copy the playlist name
                final String name = c.getString(1);

                // Create a new playlist
                final Playlist playlist = new Playlist(id, name, 0);

                // Add everything up
                usrPlaylists.add(playlist);
            } while (c.moveToNext());
        }
        // Close the cursor
        if (c != null) {
            c.close();
        }
        return usrPlaylists;
    }

}
