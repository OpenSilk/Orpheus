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

package org.opensilk.music.ui3.common;

import android.content.Context;

import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.model.Model;
import org.opensilk.music.ui3.ProfileActivity;
import org.opensilk.music.ui3.profile.ProfileScreen;

import rx.functions.Func1;

/**
 * Created by drew on 10/25/15.
 */
public class OpenProfileItemClickListener implements ItemClickListener {

    public interface ProfileScreenFactory extends Func1<Model, ProfileScreen> {

    }

    final ActivityResultsController activityResultsController;
    final ProfileScreenFactory factory;

    public OpenProfileItemClickListener(
            ActivityResultsController activityResultsController, ProfileScreenFactory factory) {
        this.activityResultsController = activityResultsController;
        this.factory = factory;
    }

    @Override
    public void onItemClicked(BundleablePresenter presenter, Context context, Model item) {
        activityResultsController.startActivityForResult(
                ProfileActivity.makeIntent(context, factory.call(item)),
                ActivityRequestCodes.PROFILE, null);
    }
}
