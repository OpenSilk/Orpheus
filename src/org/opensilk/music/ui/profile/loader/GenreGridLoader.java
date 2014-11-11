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

package org.opensilk.music.ui.profile.loader;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.loaders.WrappedAsyncTaskLoader;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.music.R;
import org.opensilk.music.util.CursorHelpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 7/16/14.
 */
public class GenreGridLoader extends WrappedAsyncTaskLoader<List<Object>> {

    private final Genre genre;

    public GenreGridLoader(Context context, Genre genre) {
        super(context);
        this.genre = genre;
    }

    @Override
    public List<Object> loadInBackground() {
        List<Object> objs = new ArrayList<>();
        // make the all songs card
        objs.add(new LocalSongGroup(getContext().getString(R.string.all_songs), genre.mGenreName, genre.mSongIds, genre.mAlbumIds));
        // make the albumcards
        Cursor c = CursorHelpers.makeLocalAlbumsCursor(getContext(), genre.mAlbumIds);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    LocalAlbum album = CursorHelpers.makeLocalAlbumFromCursor(c);
                    if (album != null) {
                        objs.add(album);
                    }
                } while (c.moveToNext());
            }
        }
        return objs;
    }
}
