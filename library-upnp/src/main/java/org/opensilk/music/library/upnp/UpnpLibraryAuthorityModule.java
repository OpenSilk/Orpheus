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

package org.opensilk.music.library.upnp;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForActivity;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.provider.LibraryProvider;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 9/21/15.
 */
@Module
public class UpnpLibraryAuthorityModule {
    @Provides @Named("UpnpLibraryBaseAuthority")
    public String provideUpnpLibraryBaseAuthority(@ForApplication Context context) {
        return context.getPackageName() + ".provider.upnp";
    }
    @Provides @Named("UpnpLibraryAuthority")
    public String provideUpnpLibraryAuthority(@Named("UpnpLibraryBaseAuthority") String base) {
        return LibraryProvider.AUTHORITY_PFX + base;
    }
}
