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

package org.opensilk.music.plugin.drive;

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.music.library.drive.DriveLibraryComponent;

/**
 * Created by drew on 6/12/14.
 */
public class App extends BaseApp {

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber(BuildConfig.DEBUG, null);
        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }
    }

    @Override
    protected Object getRootComponent() {
        return DriveLibraryComponent.FACTORY.call(this);
    }
}
