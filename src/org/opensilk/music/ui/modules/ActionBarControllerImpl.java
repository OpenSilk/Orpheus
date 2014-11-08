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

package org.opensilk.music.ui.modules;

import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import org.opensilk.music.ui.activities.BaseSlidingActivity;
import org.opensilk.music.ui.activities.HomeSlidingActivity;
import org.opensilk.common.dagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 6/16/14.
 */
public class ActionBarControllerImpl implements ActionBarController {

    private final ActionBarActivity activity;

    @Inject
    public ActionBarControllerImpl(@ForActivity ActionBarActivity activity) {
        this.activity = activity;
    }

    @Override
    public void setTitle(CharSequence title) {
        ActionBar ab = activity.getSupportActionBar();
        ab.setTitle(title);
    }

    @Override
    public void setSubTitle(CharSequence subTitle) {
        ActionBar ab = activity.getSupportActionBar();
        ab.setSubtitle(subTitle);
    }

    @Override
    public void enableHomeAsUp(int upIndicator, int icon) {
        ActionBar ab = activity.getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(upIndicator);
        ab.setIcon(icon);
    }

    @Override
    public void setBackground(Drawable background) {
        ActionBar ab = activity.getSupportActionBar();
        ab.setBackgroundDrawable(background);
    }
}
