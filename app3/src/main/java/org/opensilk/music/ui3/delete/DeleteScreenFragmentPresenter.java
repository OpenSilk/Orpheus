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

package org.opensilk.music.ui3.delete;

import android.os.Bundle;

import org.opensilk.common.core.dagger2.ScreenScope;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;

/**
 * Created by drew on 5/14/15.
 */
@ScreenScope
public class DeleteScreenFragmentPresenter extends Presenter<DeleteScreenFragment> {

    boolean dismissonload = false;

    @Inject
    public DeleteScreenFragmentPresenter() {
    }

    void dismiss() {
        if (hasView()) {
            getView().dismiss();
        } else {
            dismissonload = true;
        }
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (dismissonload) {
            dismiss();
        }
    }

    @Override
    protected BundleService extractBundleService(DeleteScreenFragment view) {
        return BundleService.getBundleService(view.getScope());
    }
}
