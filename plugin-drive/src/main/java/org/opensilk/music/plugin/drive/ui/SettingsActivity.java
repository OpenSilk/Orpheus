/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.drive.ui;

import android.app.Fragment;
import android.os.Bundle;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.plugin.common.AbsSettingsActivity;
import org.opensilk.music.plugin.drive.BuildConfig;
import org.opensilk.music.plugin.drive.GlobalComponent;

import mortar.MortarScope;

/**
 * Created by drew on 7/18/14.
 */
public class SettingsActivity extends AbsSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) { //Incase i forget to remove this. TODO remove
            getIntent().putExtra(LibraryConstants.EXTRA_LIBRARY_ID, "dr3wsuth3rland@gmail.com");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
//        builder.withService(DaggerService.DAGGER_SERVICE, ActivityComponent.FACTORY.call(
//                DaggerService.<GlobalComponent>getDaggerComponent(getApplicationContext())));
    }

    @Override
    protected Fragment getSettingsFragment(String libraryId) {
        return SettingsFragment.newInstance(libraryId);
    }

}
