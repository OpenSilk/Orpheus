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

package org.opensilk.music.ui.fragments.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.andrew.apollo.loaders.WrappedAsyncTaskLoader;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 6/24/14.
 */
public class QueueLoader extends WrappedAsyncTaskLoader<List<RecentSong>> {

    public QueueLoader(Context context) {
        super(context);
    }

    @Override
    public List<RecentSong> loadInBackground() {
        long[] list = MusicUtils.getQueue();
        final List<RecentSong> songs = new ArrayList<>(list.length);

        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        Cursor c = getContext().getContentResolver().query(MusicProvider.RECENTS_URI,
                Projections.RECENT_SONGS,
                selection.toString(),
                null,
                null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                do {
                    final RecentSong s = CursorHelpers.makeRecentSongFromCursor(c);
                    songs.add(s);
                } while (c.moveToNext());
            }
            c.close();
        }
        return songs;
    }

}
