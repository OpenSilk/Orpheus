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

import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.common.ui.mortar.LayoutCreator;
import org.opensilk.common.ui.mortar.Lifecycle;
import org.opensilk.common.ui.mortar.LifecycleService;
import org.opensilk.common.ui.mortar.ScreenScoper;

import javax.inject.Inject;

import mortar.MortarScope;
import rx.subjects.BehaviorSubject;

/**
 * Created by drew on 3/10/15.
 */
public abstract class MortarFragmentActivity extends MortarActivity
        implements FragmentManagerOwnerActivity {

    @Inject protected FragmentManagerOwner mFragmentManagerOwner;

    private final BehaviorSubject<Lifecycle> mLifecycleSubject = BehaviorSubject.create();

    protected abstract void performInjection();

    @Override
    protected void onPreCreateScope(MortarScope.Builder buidler) {
        super.onPreCreateScope(buidler);
        buidler.withService(ScreenScoper.SERVICE_NAME, new ScreenScoper())
                .withService(LayoutCreator.SERVICE_NAME, new LayoutCreator())
                .withService(LifecycleService.LIFECYCLE_SERVICE, mLifecycleSubject.asObservable())
        ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        performInjection();
        mFragmentManagerOwner.takeView(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFragmentManagerOwner.takeView(this);
        mLifecycleSubject.onNext(Lifecycle.START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFragmentManagerOwner.takeView(this);
        mLifecycleSubject.onNext(Lifecycle.RESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLifecycleSubject.onNext(Lifecycle.PAUSE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentManagerOwner.dropView(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLifecycleSubject.onNext(Lifecycle.STOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFragmentManagerOwner.dropView(this);
    }

    @Override
    public void onBackPressed() {
        if (!mFragmentManagerOwner.goBack()){
            super.onBackPressed();
        }
    }

    public final boolean isRunning() {
        return mIsResumed;
    }
}
