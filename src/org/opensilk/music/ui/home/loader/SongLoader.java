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

package org.opensilk.music.ui.home.loader;

import android.content.Context;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;

import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.Arrays;

/**
 * Created by drew on 2/18/14.
 */
public class SongLoader extends CursorLoader {

    public SongLoader(Context context) {
        super(context);
        setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        setProjection(Projections.LOCAL_SONG);
        setSelection(Selections.LOCAL_SONG);
        setSelectionArgs(SelectionArgs.LOCAL_SONG);
        setSortOrder(PreferenceUtils.getInstance(getContext()).getSongSortOrder());
    }

}
