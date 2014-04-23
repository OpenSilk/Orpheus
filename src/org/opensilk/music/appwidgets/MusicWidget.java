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

package org.opensilk.music.appwidgets;

/**
 * Created by drew on 4/22/14.
 */
public enum MusicWidget {
    ULTRA_MINI(MusicWidgetUltraMini.class),
    MINI(MusicWidgetMini.class),
    SMALL(MusicWidgetSmall.class),
    LARGE(MusicWidgetLarge.class);

    private Class<?> clzz;

    private MusicWidget(Class<?> clzz) {
        this.clzz = clzz;
    }

    public Class<?> getWidgetClass() {
        return clzz;
    }

    public static MusicWidget valueOf(int ordinal) {
        for (MusicWidget w : MusicWidget.values()) {
            if (w.ordinal() == ordinal) {
                return w;
            }
        }
        throw new IllegalArgumentException("Unknown value");
    }

}
