/*
 * Copyright (C) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.mortarfragment;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.common.ui.mortar.LayoutCreator;
import org.opensilk.common.ui.mortar.PauseAndResumeActivity;
import org.opensilk.common.ui.mortar.PauseAndResumePresenter;
import org.opensilk.common.ui.mortar.ScreenScoper;

import javax.inject.Inject;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 3/10/15.
 */
public abstract class MortarFragmentActivity extends MortarActivity
        implements FragmentManagerOwnerActivity, PauseAndResumeActivity {

    @Inject protected FragmentManagerOwner mFragmentManagerOwner;
    @Inject protected PauseAndResumePresenter mPausesAndResumesPresenter;

    protected abstract void performInjection();

    @Override
    protected void onPreCreateScope(MortarScope.Builder buidler) {
        buidler.withService(ScreenScoper.SERVICE_NAME, new ScreenScoper())
                .withService(LayoutCreator.SERVICE_NAME, new LayoutCreator());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        performInjection();
        mFragmentManagerOwner.takeView(this);
        mPausesAndResumesPresenter.takeView(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFragmentManagerOwner.takeView(this);
        mPausesAndResumesPresenter.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPausesAndResumesPresenter.activityPaused();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentManagerOwner.dropView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFragmentManagerOwner.dropView(this);
        mPausesAndResumesPresenter.dropView(this);
    }

    @Override
    public void onBackPressed() {
        if (!mFragmentManagerOwner.goBack()){
            super.onBackPressed();
        }
    }

    @Override
    public boolean isRunning() {
        return mIsResumed;
    }
}
