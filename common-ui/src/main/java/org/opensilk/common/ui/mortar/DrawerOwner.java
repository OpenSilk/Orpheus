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


import org.opensilk.common.core.dagger2.ActivityScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/**
 * Created by drew on 10/5/14.
 */
@ActivityScope
public class DrawerOwner extends Presenter<DrawerOwnerActivity> {

    @Inject
    public DrawerOwner() {
        super();
    }

    @Override
    protected BundleService extractBundleService(DrawerOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    public void openDrawer() {
        DrawerOwnerActivity v = getView();
        if (v == null) return;
        v.openDrawer();
    }

    public void closeDrawer() {
        DrawerOwnerActivity v = getView();
        if (v == null) return;
        v.closeDrawer();
    }

    public void disableDrawer(boolean hideIndicator) {
        DrawerOwnerActivity v = getView();
        if (v == null) return;
        v.disableDrawer(hideIndicator);
    }

    public void enableDrawer() {
        DrawerOwnerActivity v = getView();
        if (v == null) return;
        v.enableDrawer();
    }

}
