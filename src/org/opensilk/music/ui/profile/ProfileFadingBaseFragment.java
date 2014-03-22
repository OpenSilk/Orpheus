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

package org.opensilk.music.ui.profile;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;

import com.andrew.apollo.utils.ThemeHelper;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;

/**
 * Created by drew on 2/23/14.
 */
public abstract class ProfileFadingBaseFragment<D extends Parcelable> extends ProfileBaseFragment<D> {

    /* Manages our views */
    protected FadingActionBarHelper mFadingHelper;
    /* header image */
    protected ProfileHeaderImage mHeaderImage;

    protected Drawable mActionBarBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBarBackground = ThemeHelper.getInstance(getActivity()).getActionBarBackground();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset our action bar color XXX it appears mActionBarBackground get gc'd before we reach here
        getActivity().getActionBar().setBackgroundDrawable(ThemeHelper.getInstance(getActivity()).getActionBarBackground());
        mFadingHelper = null;
        mHeaderImage = null;
    }

}
