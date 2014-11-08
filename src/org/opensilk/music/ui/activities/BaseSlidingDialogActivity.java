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

package org.opensilk.music.ui.activities;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.common.dagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 9/18/14.
 */
public class BaseSlidingDialogActivity extends BaseSlidingActivity {

    @Inject @ForActivity
    protected ActionBarController mActionBarHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.getInstance(this).getPanelDialogTheme();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dialogsliding);

//        mActionBarHelper.enableHomeAsUp(R.drawable.blank,
//                mIsDialog ? R.drawable.ic_action_cancel_white : R.drawable.ic_action_arrow_left_white);

    }

}
