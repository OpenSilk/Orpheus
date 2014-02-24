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

import android.os.Parcelable;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.manuelpeinado.fadingactionbar.FadingActionBarHelper;

/**
 * Created by drew on 2/23/14.
 */
public abstract class ProfileFadingBaseFragment<D extends Parcelable> extends ProfileBaseFragment<D> {

    /* Manages our views */
    protected FadingActionBarHelper mFadingHelper;
    /* header image */
    protected ImageView mHeaderImage;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset our action bar color
        getActivity().getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid_orpheus));
        mFadingHelper = null;
        mHeaderImage = null;
    }

}
