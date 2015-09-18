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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Gravity;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.util.VersionUtils;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;
import timber.log.Timber;

/**
 * Created by drew on 3/10/15.
 */
@ActivityScope
public class FragmentManagerOwner extends Presenter<FragmentManagerOwnerActivity> {

    @Inject
    public FragmentManagerOwner() {
    }

    @Override
    protected BundleService extractBundleService(FragmentManagerOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    public FragmentTransaction newTrasaction() {
        if (hasView()) {
            return getView().getSupportFragmentManager().beginTransaction();
        }
        throw new IllegalStateException("No view");
    }

    public void showDialog(MortarDialogFragment f) {
        if (hasView()) {
            f.show(getView().getSupportFragmentManager(), f.getScopeName());
        }
    }

    public int addFragment(MortarFragment frag, boolean addToBackstack) {
        if (!hasView()) return -1;
        String tag = frag.getScopeName();
        FragmentTransaction ft = getView().getSupportFragmentManager().beginTransaction();
        if (!VersionUtils.hasLollipop()) {
            ft.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
        ft.add(frag, tag);
        if (addToBackstack) ft.addToBackStack(tag);
        return ft.commit();
    }

    public int replaceMainContent(MortarFragment frag, boolean addToBackstack) {
        if (!hasView()) return -1;
        String tag = frag.getScopeName();
        FragmentTransaction ft = getView().getSupportFragmentManager().beginTransaction();
        if (!VersionUtils.hasLollipop()) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.replace(getView().getContainerViewId(), frag, tag);
        if (addToBackstack) ft.addToBackStack(tag);
        return ft.commit();
    }

    public boolean goBack() {
        if (hasView() && getView().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            return getView().getSupportFragmentManager().popBackStackImmediate();
        }
        return false;
    }

    public void killBackStack() {
        if (hasView() && getView().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getView().getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

}
