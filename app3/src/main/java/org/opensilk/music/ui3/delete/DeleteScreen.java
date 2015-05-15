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

package org.opensilk.music.ui3.delete;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.model.ex.BadBundleableException;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.model.util.BundleableUtil;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableScreen;

import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 5/14/15.
 */
@Layout(R.layout.screen_delete)
@WithComponentFactory(DeleteScreen.Factory.class)
public class DeleteScreen extends BundleableScreen implements Parcelable {

    final Bundleable bundleable;
    final String what;
    final List<Uri> trackUris;

    public DeleteScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo, Bundleable bundleable) {
        super(libraryConfig, libraryInfo);
        this.bundleable = bundleable;
        this.what = null;
        this.trackUris = null;
    }

    public DeleteScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo, String what, List<Uri> trackUris) {
        super(libraryConfig, libraryInfo);
        this.bundleable = null;
        this.what = what;
        this.trackUris = trackUris;
    }

    private DeleteScreen(LibraryConfig libraryConfig, LibraryInfo libraryInfo,
                         Bundleable bundleable, String what, List<Uri> trackUris) {
        super(libraryConfig, libraryInfo);
        this.bundleable = bundleable;
        this.what = what;
        this.trackUris = trackUris;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(libraryConfig.dematerialize());
        dest.writeParcelable(libraryInfo, flags);
        dest.writeBundle(bundleable != null ? bundleable.toBundle() : null);
        dest.writeString(what);
        dest.writeTypedList(trackUris);
    }

    public static final Creator<DeleteScreen> CREATOR = new Creator<DeleteScreen>() {
        @Override
        public DeleteScreen createFromParcel(Parcel source) {
            LibraryConfig config = LibraryConfig.materialize(source.readBundle());
            LibraryInfo info = source.readParcelable(getClass().getClassLoader());
            Bundle bundle = source.readBundle();
            Bundleable bundleable = null;
            if (bundle != null) {
                try {
                    bundleable = BundleableUtil.materializeBundle(bundle);
                } catch (BadBundleableException e) {
                    bundleable = null;
                }
            }
            String what = source.readString();
            List<Uri> uris = source.createTypedArrayList(Uri.CREATOR);
            return new DeleteScreen(config, info, bundleable, what, uris);
        }

        @Override
        public DeleteScreen[] newArray(int size) {
            return new DeleteScreen[size];
        }
    };

    public static class Factory extends ComponentFactory<DeleteScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, DeleteScreen screen) {
            MusicActivityComponent activityComponent = DaggerService.getDaggerComponent(parentScope);
            return DeleteScreenComponent.FACTORY.call(activityComponent, screen);
        }
    }
}
