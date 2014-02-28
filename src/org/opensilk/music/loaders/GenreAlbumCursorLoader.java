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
import android.support.v4.content.CursorLoader;

import com.andrew.apollo.provider.MusicProvider;

/**
 * Created by drew on 2/28/14.
 */
public class GenreAlbumCursorLoader extends CursorLoader {

    public GenreAlbumCursorLoader(Context context, long genreId) {
        super(context);
        setUri(MusicProvider.makeGenreAlbumsUri(genreId));
        // our content provider doesnt read any of these
        setProjection(null);
        setSelection(null);
        setSelectionArgs(null);
        setSortOrder(null);
    }

}
