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

package org.opensilk.music.ui2.util;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by drew on 10/9/14.
 */
public class ViewStateSaver {

    private ViewStateSaver() {}

    public static <T extends View> void save(T view, Bundle bundle, String name) {
        if (view == null) return;
        SparseArray<Parcelable> state = new SparseArray<>();
        view.saveHierarchyState(state);
        bundle.putSparseParcelableArray(name, state);
    }

    public static <T extends View> void restore(T view, Bundle bundle, String name) {
        if (bundle == null || view == null) return;
        SparseArray<Parcelable> state = bundle.getSparseParcelableArray(name);
        if (state != null) view.restoreHierarchyState(state);
    }

    public static <T extends View> T inflate(Context context, int layout, ViewGroup parent) {
        return inflate(context, layout, parent, false);
    }

    public static <T extends View> T inflate(Context context, int layout, ViewGroup parent, boolean attachToRoot) {
        return (T) LayoutInflater.from(context).inflate(layout, parent, attachToRoot);
    }
}
