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

package org.opensilk.common.ui.mortar;

import android.content.Context;
import android.view.Menu;

import org.opensilk.common.core.util.Preconditions;

import java.util.Arrays;

import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
public class ActionBarMenuConfig {
    public final Func2<Context, Integer, Boolean> actionHandler;
    public final Integer[] menus;
    public final CustomMenuItem[] customMenus;

    private ActionBarMenuConfig(
            Func2<Context, Integer, Boolean> actionHandler,
            Integer[] menus,
            CustomMenuItem[] customMenus
    ) {
        this.actionHandler = actionHandler;
        this.menus = menus;
        this.customMenus = customMenus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class CustomMenuItem {
        public final int groupId;
        public final int itemId;
        public final int order;
        public final CharSequence title;
        public final int iconRes;

        public CustomMenuItem(int itemId, CharSequence title) {
            this.groupId = Menu.NONE;
            this.itemId = itemId;
            this.order = Menu.NONE;
            this.title = title;
            this.iconRes = -1;
        }

        public CustomMenuItem(int groupId, int itemId, int order, CharSequence title, int iconRes) {
            this.groupId = groupId;
            this.itemId = itemId;
            this.order = order;
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    public static class Builder {
        public Func2<Context, Integer, Boolean> actionHandler;
        public Integer[] menus = new Integer[0];
        public CustomMenuItem[] customMenus = new CustomMenuItem[0];

        private Builder() {
        }

        public Builder setActionHandler(Func2<Context, Integer, Boolean> actionHandler) {
            this.actionHandler = actionHandler;
            return this;
        }

        public Builder withMenu(int menu) {
            this.menus = concatArrays(this.menus, new Integer[]{menu});
            return this;
        }

        public Builder withMenus(Integer[] menus) {
            this.menus = concatArrays(this.menus, menus);
            return this;
        }

        public Builder withMenu(CustomMenuItem item) {
            this.customMenus = concatArrays(this.customMenus, new CustomMenuItem[]{item});
            return this;
        }

        public Builder withMenus(CustomMenuItem[] customMenus) {
            this.customMenus = concatArrays(this.customMenus, customMenus);
            return this;
        }

        public ActionBarMenuConfig build() {
            Preconditions.checkNotNull(actionHandler, "Must set actionHandler");
            return new ActionBarMenuConfig(actionHandler, menus, customMenus);
        }
    }

    protected static <T> T[] concatArrays(T[] a1, T[] a2) {
        if (a1.length == 0) return a2;
        if (a2.length == 0) return a1;
        T a3[] = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, a3, a1.length, a2.length);
        return a3;
    }

    /*
     * From commons-lang
     */

    /**
     * <p>Converts an array of primitive ints to objects.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  an {@code int} array
     * @return an {@code Integer} array, {@code null} if null array input
     */
    public static Integer[] toObject(final int[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new Integer[0];
        }
        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Integer.valueOf(array[i]);
        }
        return result;
    }

    /**
     * <p>Converts an array of object Integer to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Integer} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return an {@code int} array, {@code null} if null array input
     */
    public static int[] toPrimitive(final Integer[] array, final int valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new int[0];
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            final Integer b = array[i];
            result[i] = (b == null ? valueForNull : b.intValue());
        }
        return result;
    }
}
