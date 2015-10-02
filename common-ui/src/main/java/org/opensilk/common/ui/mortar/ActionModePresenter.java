/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.ui.mortar;

import android.os.Bundle;
import android.support.v7.view.ActionMode;

import org.opensilk.common.core.dagger2.ActivityScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/**
 * Created by drew on 10/1/15.
 */
@ActivityScope
public class ActionModePresenter extends Presenter<ActionModeActivity> {

    @Inject
    public ActionModePresenter() {
    }

    @Override
    protected BundleService extractBundleService(ActionModeActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (hasView()) {
            return getView().startSupportActionMode(callback);
        }
        return null;
    }

    public void cancelActionMode() {
        if (hasView()) {
            getView().cancelActionMode();
        }
    }

}
