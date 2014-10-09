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

package org.opensilk.music.ui2.main;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.MortarScope;
import mortar.Presenter;

/**
 * Created by drew on 10/5/14.
 */
@Singleton
public class DrawerPresenter extends Presenter<DrawerPresenter.View> {

    public interface View extends HasScope {
        void openDrawer();
        void closeDrawer();
        void disableDrawer(boolean hideIndicator);
        void enableDrawer();
    }

    @Inject
    public DrawerPresenter() {
        super();
    }

    @Override
    protected MortarScope extractScope(View view) {
        return view.getScope();
    }

    public void openDrawer() {
        View v = getView();
        if (v == null) return;
        v.openDrawer();
    }

    public void closeDrawer() {
        View v = getView();
        if (v == null) return;
        v.closeDrawer();
    }

    public void disableDrawer(boolean hideIndicator) {
        View v = getView();
        if (v == null) return;
        v.disableDrawer(hideIndicator);
    }

    public void enableDrawer() {
        View v = getView();
        if (v == null) return;
        v.enableDrawer();
    }

}
