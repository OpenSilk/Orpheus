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

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.opensilk.common.core.dagger2.ActivityScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;
import timber.log.Timber;

/** Allows shared configuration of the Android ActionBar. */
@ActivityScope
public class ToolbarOwner extends Presenter<ToolbarOwnerActivity> {

    private Toolbar toolbar;

    @Inject
    public ToolbarOwner() {
    }

    @Override
    protected BundleService extractBundleService(ToolbarOwnerActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    public void attachToolbar(Toolbar toolbar) {
        if (toolbar == null) {
            throw new IllegalArgumentException("Toolbar may not be null");
        }
        if (this.toolbar != toolbar) {
            if (this.toolbar != null) {
                detachToolbar(this.toolbar);
            }
            this.toolbar = toolbar;
            if (hasView()) {
                getView().setSupportActionBar(toolbar);
                getView().onToolbarAttached(toolbar);
            }
        }
    }

    public void detachToolbar(Toolbar toolbar) {
        if (toolbar == null) {
            throw new IllegalArgumentException("Toolbar may not be null");
        }
        if (this.toolbar == toolbar) {
            this.toolbar = null;
            if (hasView()) {
//                getView().setSupportActionBar(null);
                getView().onToolbarDetached(toolbar);
            } else {
                Timber.e("detachToolbar called after dropView");
            }
        }
    }

    public void setConfig(ActionBarConfig config) {
        if (!hasView()) {
            Timber.w("setConfig() called without view");
            return;
        }
        if (config.titleRes >= 0) {
            toolbar.setTitle(config.titleRes);
        } else {
            toolbar.setTitle(config.title);
        }
        if (config.subtitleRes >= 0) {
            toolbar.setSubtitle(config.subtitleRes);
        } else {
            toolbar.setSubtitle(config.subtitle);
        }
        getView().setToolbarMenu(config.menuConfig);
    }

}
