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

package org.opensilk.common.core.dagger2;

import android.content.Context;

/**
 * Created by drew on 4/28/15.
 */
public class AppContextComponentHolder {
    private static AppContextComponentHolder instance;

    private final AppContextComponent appContextComponent;

    private AppContextComponentHolder(Context appContext) {
        appContextComponent = DaggerAppContextComponent.builder()
                .appContextModule(new AppContextModule(appContext))
                .build();
    }

    public static AppContextComponent getAppContextComponent(Context context) {
        if (instance == null) {
            instance = new AppContextComponentHolder(context.getApplicationContext());
        }
        return instance.appContextComponent;
    }
}
