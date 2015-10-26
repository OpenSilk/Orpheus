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

import org.opensilk.common.core.dagger2.ActivityScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/**
 * Dialogs *must* be dismissed on configuration changes
 * presenters cant do this so their views must manage the dialogs
 * this gets rather annoying so this class tries to abstract
 * that functionality to the activity.
 * Created by drew on 10/15/15.
 */
@ActivityScope
public class DialogPresenter extends Presenter<DialogPresenterActivity> {

    @Inject
    public DialogPresenter() {
    }

    @Override
    protected BundleService extractBundleService(DialogPresenterActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    public void showDialog(DialogFactory factory) {
        if (hasView()) {
            getView().showDialog(factory);
        }
    }

    public void dismissDialog() {
        if (hasView()) {
            getView().dismissDialog();
        }
    }

}
