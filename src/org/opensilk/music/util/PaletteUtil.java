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

package org.opensilk.music.util;

import android.support.v7.graphics.Palette;

/**
 * Created by drew on 8/29/14.
 */
public class PaletteUtil {

    public static Palette.Swatch getBackgroundItem(Palette palette) {
        Palette.Swatch item = null;
        if (palette != null) {
            item = palette.getVibrantSwatch();
            if (item == null) {
                item = palette.getMutedSwatch();
            }
        }
        return item;
    }

}
