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

package org.opensilk.music.ui3.library;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 9/9/15.
 */
public class ProviderInfoBundleable implements Bundleable {

    final LibraryProviderInfo info;
    boolean isLoading;
    boolean needsLogin;

    public ProviderInfoBundleable(LibraryProviderInfo info) {
        this.info = info;
        isLoading = true;
        needsLogin = false;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
    }

    public boolean isNeedsLogin() {
        return needsLogin;
    }

    public void setNeedsLogin(boolean needsLogin) {
        this.needsLogin = needsLogin;
    }

    public String getAuthority() {
        return info.getAuthority();
    }

    public Drawable getIcon() {
        return info.getIcon();
    }

    public LibraryProviderInfo getInfo() {
        return info;
    }

    @Override
    public Bundle toBundle() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String getIdentity() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String getName() {
        return getDisplayName();
    }

    @Override
    public Uri getUri() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String getDisplayName() {
        return info.getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderInfoBundleable that = (ProviderInfoBundleable) o;
        return info.equals(that.info);
    }

    @Override
    public int hashCode() {
        return info.hashCode();
    }
}
