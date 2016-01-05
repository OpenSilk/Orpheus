/*
 * Copyright (c) 2016 OpenSilk Productions LLC
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

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.model.Container;
import org.opensilk.music.ui3.MusicActivityComponent;

import java.util.ArrayList;
import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 1/4/16.
 */
@WithComponentFactory(LibraryOpScreen.Factory.class)
public class LibraryOpScreen extends Screen implements Parcelable {

    public static LibraryOpScreen unIndexOp(List<Container> containers) {
        List<Bundle> bundles = new ArrayList<>(containers.size());
        for (Container c : containers) {
            bundles.add(c.toBundle());
        }
        return new LibraryOpScreen(Op.UNINDEX,
                BundleHelper.b().putList(bundles).get());
    }

    public static LibraryOpScreen deleteOp(List<Uri> uris, Uri notifyUri) {
        return new LibraryOpScreen(Op.DELETE,
                BundleHelper.b().putList(uris).putUri(notifyUri).get());
    }

    public static LibraryOpScreen getContainerOp(Uri containerUri) {
        return new LibraryOpScreen(Op.GET_CONTAINER,
                BundleHelper.b().putUri(containerUri).get());
    }

    public enum Op {
        UNINDEX,
        DELETE,
        GET_CONTAINER,
    }

    final Op op;
    final Bundle extras;

    public LibraryOpScreen(Op op, Bundle extras) {
        this.op = op;
        this.extras = extras;
    }

    @Override
    public String getName() {
        return super.getName() + op.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(op);
        dest.writeBundle(extras);
    }

    public static final Creator<LibraryOpScreen> CREATOR = new Creator<LibraryOpScreen>() {
        @Override
        public LibraryOpScreen createFromParcel(Parcel source) {
            final Op op = (Op) source.readSerializable();
            final Bundle b = source.readBundle();
            return new LibraryOpScreen(op, b);
        }

        @Override
        public LibraryOpScreen[] newArray(int size) {
            return new LibraryOpScreen[size];
        }
    };

    public static final class Factory extends ComponentFactory<LibraryOpScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, LibraryOpScreen screen) {
            MusicActivityComponent cmp = DaggerService.getDaggerComponent(parentScope);
            return LibraryOpScreenComponent.FACTORY.call(cmp, screen);
        }
    }
}
