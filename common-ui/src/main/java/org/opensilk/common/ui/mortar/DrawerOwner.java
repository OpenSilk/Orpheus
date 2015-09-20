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

package org.opensilk.common.ui.mortar;


import android.support.v4.widget.DrawerLayout;
import android.view.View;

import org.opensilk.common.core.dagger2.ActivityScope;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.Presenter;
import mortar.Scoped;
import mortar.bundler.BundleService;

/**
 * A bridge between presenters and the host activity to propagate
 * drawer actions/events. It is the activities responibility
 * to call all the {@link android.support.v4.widget.DrawerLayout.DrawerListener}
 * methods from the {@link android.support.v7.app.ActionBarDrawerToggle.DrawerToggle}.
 * Presenters may register their own listeners with us and events will propagate to them.
 *
 * Created by drew on 10/5/14.
 */
@ActivityScope
public class DrawerOwner extends Presenter<DrawerOwnerActivity>
        implements DrawerLayout.DrawerListener, DrawerListenerRegistrar, DrawerController {

    private final Set<Registration> registrations = new HashSet<>();

    @Inject
    public DrawerOwner() {
    }

    @Override
    protected BundleService extractBundleService(DrawerOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override
    public void onExitScope() {
        registrations.clear();
    }

    @Override
    public void openDrawer(int gravity) {
        if (hasView()) getView().openDrawer(gravity);
    }

    @Override
    public void openDrawers() {
        if (hasView()) getView().openDrawers();
    }

    @Override
    public void closeDrawer(int gravity) {
        if (hasView()) getView().closeDrawer(gravity);
    }

    @Override
    public void closeDrawers() {
        if (hasView()) getView().closeDrawers();
    }

    @Override
    public void enableDrawer(int gravity, boolean enable) {
        if (hasView()) getView().enableDrawer(gravity, enable);
    }

    @Override
    public void enableDrawers(boolean enable) {
        if (hasView()) getView().enableDrawers(enable);
    }

    @Override
    public void register(MortarScope scope, DrawerLayout.DrawerListener listener) {
        Registration registration = new Registration(listener);
        scope.register(registration);

        boolean added = registrations.add(registration);
//        if (added && isRunning()) {
//            listener.onResume();
//        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        for (Registration registration : registrations) {
            registration.registrant.onDrawerSlide(drawerView, slideOffset);
        }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        for (Registration registration : registrations) {
            registration.registrant.onDrawerOpened(drawerView);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        for (Registration registration : registrations) {
            registration.registrant.onDrawerClosed(drawerView);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        for (Registration registration : registrations) {
            registration.registrant.onDrawerStateChanged(newState);
        }
    }

    private class Registration implements Scoped {
        final DrawerLayout.DrawerListener registrant;

        private Registration(DrawerLayout.DrawerListener registrant) {
            this.registrant = registrant;
        }

        @Override public void onEnterScope(MortarScope scope) {
        }

        @Override public void onExitScope() {
            registrations.remove(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Registration that = (Registration) o;

            return registrant.equals(that.registrant);
        }

        @Override
        public int hashCode() {
            return registrant.hashCode();
        }
    }

}
