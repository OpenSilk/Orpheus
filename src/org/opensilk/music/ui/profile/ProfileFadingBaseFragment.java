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

import org.opensilk.music.widgets.BottomCropArtworkImageView;

/**
 * Created by drew on 2/23/14.
 */
public abstract class ProfileFadingBaseFragment<D extends Parcelable> extends ProfileBaseFragment<D> {

    /* Manages our views */
    protected FadingActionBarHelper mFadingHelper;
    /* header image */
    protected BottomCropArtworkImageView mHeaderImage;

    protected Drawable mActionBarBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBarBackground = ThemeHelper.getActionBarBackground(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFadingHelper = null;
        mHeaderImage = null;
    }

}
