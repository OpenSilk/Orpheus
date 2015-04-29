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

package org.opensilk.music.plugin.drive;

import android.app.Application;
import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.plugin.drive.ui.AuthTestFragment;
import org.opensilk.music.plugin.drive.ui.SettingsActivity;
import org.opensilk.music.plugin.drive.util.DriveHelper;
import org.opensilk.music.plugin.drive.util.DriveHelperImpl;

import javax.inject.Singleton;

import dagger.Provides;

/**
 * Created by drew on 4/28/15.
 */
@dagger.Module
public class AppModule {
    private final Application app;

    public AppModule(Application app) {
        this.app = app;
    }

    @Provides @Singleton
    public DriveHelper provideDriveApi(DriveHelperImpl api) {
        return api;
    }
}
