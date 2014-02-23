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

import android.provider.BaseColumns;
import android.provider.MediaStore;

/**
 * Created by drew on 2/22/14.
 */
public class Projections {

    private Projections() {
        //static
    }

    public static final String[] SONG;

    static {
        SONG = new String[] {
                        /* 0 */
                BaseColumns._ID,
                        /* 1 */
                MediaStore.Audio.AudioColumns.TITLE,
                        /* 2 */
                MediaStore.Audio.AudioColumns.ARTIST,
                        /* 3 */
                MediaStore.Audio.AudioColumns.ALBUM,
                        /* 4 */
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                        /* 5 */
                MediaStore.Audio.AudioColumns.DURATION
        };
    }

}
