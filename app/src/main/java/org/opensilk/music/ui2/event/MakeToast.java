/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.event;

/**
 * Created by drew on 10/27/14.
 */
public class MakeToast {
    public enum Type {
        NORMAL,
        PLURALS
    }

    public final Type type;
    public final int resId;
    public final Object[] params;
    public final int arg;

    public MakeToast(int resId) {
        this.type = Type.NORMAL;
        this.resId = resId;
        this.params = new Object[0];
        this.arg = -1;
    }

    public MakeToast(int resId, Object... params) {
        this.type = Type.NORMAL;
        this.resId = resId;
        this.params = params;
        this.arg = -1;
    }

    public MakeToast(int resId, int arg) {
        this.type = Type.PLURALS;
        this.resId = resId;
        this.params = new Object[0];
        this.arg = arg;
    }
}
