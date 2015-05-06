/*
 * Copyright 2013 Square Inc.
 * Copyright (C) 2014 OpenSilk Productions LLC
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

package org.opensilk.common.ui.mortar;

import android.os.Bundle;

import org.opensilk.common.core.dagger2.ActivityScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/** Allows shared configuration of the Android ActionBar. */
@ActivityScope
public class ActionBarOwner extends Presenter<ActionBarOwnerActivity> {

    private ActionBarConfig config;

    @Inject
    public ActionBarOwner() {
        super();
    }

    @Override
    protected BundleService extractBundleService(ActionBarOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override public void onLoad(Bundle savedInstanceState) {
        if (config != null) update();
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        config = null;
    }

    public void setConfig(ActionBarConfig config) {
        this.config = config;
        update();
    }

    public ActionBarConfig getConfig() {
        return config;
    }

    private void update() {
        ActionBarOwnerActivity view = getView();
        if (!hasView()) {
            return;
        }
        view.setUpButtonEnabled(config.upButtonEnabled);
        if (config.titleRes >= 0) {
            view.setTitle(config.titleRes);
        } else {
            view.setTitle(config.title);
        }
        if (config.subtitleRes >= 0) {
            view.setSubtitle(config.subtitleRes);
        } else {
            view.setSubtitle(config.subtitle);
        }
        view.setMenu(config.menuConfig);
        view.setTransparentActionbar(config.transparentBackground);
    }

}
