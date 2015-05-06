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

package org.opensilk.common.ui.util;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.ui.mortar.Layout;

/**
 * Created by drew on 10/9/14.
 */
public class ViewUtils {

    private ViewUtils() {}

    public static <T extends View> void saveState(T view, Bundle bundle, String name) {
        if (view == null) return;
        bundle.putSparseParcelableArray(name, saveState(view));
    }

    public static <T extends View> SparseArray<Parcelable> saveState(T view) {
        SparseArray<Parcelable> state = new SparseArray<>();
        view.saveHierarchyState(state);
        return state;
    }

    public static <T extends View> void restoreState(T view, Bundle bundle, String name) {
        if (bundle == null || view == null) return;
        SparseArray<Parcelable> state = bundle.getSparseParcelableArray(name);
        if (state != null) view.restoreHierarchyState(state);
    }

    /** Note this will attach the view to the container use the other method to disable that */
    public static <T extends View> T inflate(Context context, int layout, ViewGroup parent) {
        return (T) LayoutInflater.from(context).inflate(layout, parent);
    }

    public static <T extends View> T inflate(Context context, int layout, ViewGroup parent, boolean attachToRoot) {
        return (T) LayoutInflater.from(context).inflate(layout, parent, attachToRoot);
    }

    /*
     * Flow helpers see {@link flow.Layouts}
     */

    /** Create an instance of the view specified in a {@link flow.Layout} annotation. */
    public static <T extends View> T createView(Context context, Object screen, ViewGroup container) {
        return createView(context, screen.getClass(), container);
    }

    /** Create an instance of the view specified in a {@link flow.Layout} annotation. */
    public static <T extends View> T createView(Context context, Class<?> screenType, ViewGroup container) {
        Layout screen = screenType.getAnnotation(Layout.class);
        if (screen == null) {
            throw new IllegalArgumentException(
                    String.format("@%s annotation not found on class %s", Layout.class.getSimpleName(),
                            screenType.getName()));
        }
        int layout = screen.value();
        return inflate(context, layout, container);
    }

}
