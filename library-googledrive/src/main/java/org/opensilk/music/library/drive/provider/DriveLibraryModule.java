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

package org.opensilk.music.library.drive.provider;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.drive.DriveAuthorityModule;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/20/15.
 */
@Module(
        includes = DriveAuthorityModule.class
)
public class DriveLibraryModule {

    @Provides @DriveLibraryScope
    public HttpTransport provideHttpTransport() {
        return AndroidHttp.newCompatibleTransport();
    }

    @Provides @DriveLibraryScope
    public JsonFactory provideJsonFactory() {
        return AndroidJsonFactory.getDefaultInstance();
    }

    @Provides @DriveLibraryScope @Named("AppIdentifier")
    public String provideAppIdentifier(@ForApplication Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.packageName + "/" + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //better never happen we're calling ourselves
            throw new RuntimeException(e);
        }
    }
}
