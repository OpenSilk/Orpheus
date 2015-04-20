/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.main;

import android.graphics.drawable.Drawable;

import org.opensilk.common.flow.Screen;

/**
 * Created by drew on 4/20/15.
 */
public class NavItem {
    public enum Type {
        HEADER,
        ITEM,
    }

    public final Type type;
    public final int titleRes;
    public final CharSequence title;
    public final int iconRes;
    public final Drawable icon;
    public final Screen screen;
    public final Object event;

    public NavItem(Type type, int titleRes, CharSequence title,
                   int iconRes, Drawable icon, Screen screen, Object event) {
        this.type = type;
        this.titleRes = titleRes;
        this.title = title;
        this.iconRes = iconRes;
        this.icon = icon;
        this.screen = screen;
        this.event = event;
    }

    static class Builder {
        Type type = null;
        int titleRes = -1;
        CharSequence title = null;
        int iconRes = -1;
        Drawable icon = null;
        Screen screen = null;
        Object event = null;

        NavItem build() {
            return new NavItem(type, titleRes, title, iconRes, icon, screen, event);
        }
    }

    public static class Factory {

        public static NavItem newHeader(int titleRes, int iconRes, Object event) {
            Builder b = new Builder();
            b.type = Type.HEADER;
            b.titleRes = titleRes;
            b.iconRes = iconRes;
            b.event = event;
            return b.build();
        }

        public static NavItem newItem(CharSequence title, Drawable icon, Screen screen) {
            Builder b = new Builder();
            b.type = Type.ITEM;
            b.title = title;
            b.icon = icon;
            b.screen = screen;
            return b.build();
        }

        public static NavItem newItem(int titleRes, int iconRes, Screen screen) {
            Builder b = new Builder();
            b.type = Type.ITEM;
            b.titleRes = titleRes;
            b.iconRes = iconRes;
            b.screen = screen;
            return b.build();
        }
    }

}
