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
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

/**
 * Created by drew on 2/28/14.
 */
public class GenreSongLoader extends CursorLoader {

    public GenreSongLoader(Context context, long genreId) {
        super(context);
        setUri(MediaStore.Audio.Genres.Members.getContentUri("external", genreId));
        setProjection(Projections.PLAYLIST_SONGS); //We're the same, remap audio_id to _id
        setSelection(Selections.LOCAL_SONG);
        setSelectionArgs(SelectionArgs.LOCAL_SONG);
        setSortOrder(MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);
    }

}
