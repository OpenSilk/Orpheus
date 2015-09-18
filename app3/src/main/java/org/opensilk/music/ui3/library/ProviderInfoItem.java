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
import org.opensilk.music.model.Container;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 9/9/15.
 */
public class ProviderInfoItem {

    final LibraryProviderInfo info;
    final List<Container> roots = new ArrayList<>();

    boolean isLoading = true;
    boolean needsLogin = false;

    public ProviderInfoItem(LibraryProviderInfo info) {
        this.info = info;
    }

    public LibraryProviderInfo getInfo() {
        return info;
    }

    public List<Container> getRoots() {
        return roots;
    }

    public void setLoading(boolean yes) {
        isLoading = yes;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setNeedsLogin(boolean yes) {
        needsLogin = yes;
    }

    public boolean needsLogin() {
        return needsLogin;
    }
}
