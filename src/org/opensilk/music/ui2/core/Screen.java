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

package org.opensilk.music.ui2.core;

import android.os.Parcelable;
import android.util.SparseArray;

/**
 * Created by drew on 10/13/14.
 */
public class Screen {

    private int[]                   transitions;
    private SparseArray<Parcelable> viewState;

    public final void setTransitions(int[] transitions) {
        this.transitions = transitions;
    }

    public final int[] getTransitions() { return transitions; }

    public final void setViewState(SparseArray<Parcelable> viewStateToSave) {
        viewState = viewStateToSave;
    }

    public final SparseArray<Parcelable> getViewState() {
        return viewState;
    }

}
