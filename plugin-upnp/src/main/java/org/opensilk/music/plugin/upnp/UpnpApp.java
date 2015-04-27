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

package org.opensilk.music.plugin.upnp;

import android.content.Context;
import android.os.StrictMode;

import org.opensilk.common.dagger.DaggerApplication;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import dagger.Provides;
import timber.log.Timber;

/**
 * Created by drew on 6/9/14.
 */
public class UpnpApp extends DaggerApplication {

    @dagger.Module(
            injects = {
                    UpnpLibraryService.class,
            },
            library = true
    )
    public static class Module {
        public UpnpApp app;

        public Module(UpnpApp app) {
            this.app = app;
        }

        @Provides @Singleton @ForApplication
        public Context provideAppContext() {
            return app;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
                new org.seamless.android.FixedAndroidLogHandler()
        );

        if (BuildConfig.DEBUG) {
            // enable logging as needed for various categories of Cling:
            Logger.getLogger("org.fourthline.cling").setLevel(Level.FINE);
            Logger.getLogger("org.fourthline.cling.transport.spi.DatagramProcessor").setLevel(Level.INFO);
            Logger.getLogger("org.fourthline.cling.protocol.ProtocolFactory").setLevel(Level.INFO);
            Logger.getLogger("org.fourthline.cling.model.message.UpnpHeaders").setLevel(Level.INFO);
//            Logger.getLogger("org.fourthline.cling.transport.spi.SOAPActionProcessor").setLevel(Level.FINER);

            // Plant the forest
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
        } else {
            // enable logging as needed for various categories of Cling:
//            Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

            // Plant the forest
//            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new Module(this),
        };
    }
}
