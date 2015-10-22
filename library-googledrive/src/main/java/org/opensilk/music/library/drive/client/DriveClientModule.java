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

package org.opensilk.music.library.drive.client;

import android.content.Context;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.drive.Constants;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/20/15.
 */
@Module
public class DriveClientModule {
    final String accountName;

    public DriveClientModule(String accountName) {
        this.accountName = accountName;
    }

    @Provides @DriveClientScope
    public GoogleAccountCredential provideCredential(@ForApplication Context context) {
        return GoogleAccountCredential.usingOAuth2(context, Constants.SCOPES)
                .setSelectedAccountName(accountName);
    }
}
