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

import android.app.Application;
import android.os.StrictMode;

import org.opensilk.common.core.mortar.DaggerService;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 6/12/14.
 */
public class App extends Application {

    private MortarScope mRootScope;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            // logging
            Timber.plant(new Timber.DebugTree());

            // enable strict mode
            final StrictMode.ThreadPolicy.Builder threadPolicyBuilder
                    = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen();
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());

            final StrictMode.VmPolicy.Builder vmPolicyBuilder
                    = new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog();
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        } //else TODO
    }

    @Override
    public Object getSystemService(String name) {
        if (mRootScope == null) {
            mRootScope = MortarScope.buildRootScope()
                    .withService(DaggerService.DAGGER_SERVICE, GlobalComponent.FACTORY.call(this))
                    .build("ROOT");
        }
        if (mRootScope.hasService(name)) {
            return mRootScope.getService(name);
        }
        return super.getSystemService(name);
    }
}
