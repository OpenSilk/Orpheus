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

package org.opensilk.music.library.client;

import android.content.Context;
import android.net.Uri;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;

import rx.Scheduler;

/**
 * Created by drew on 5/2/15.
 */
public class BundleableLoader extends TypedBundleableLoader<Bundleable> {

    @Inject
    public BundleableLoader(@ForApplication Context context) {
        super(context);
    }

    public static BundleableLoader create(Context context) {
        return new BundleableLoader(context.getApplicationContext());
    }

    @Override
    public BundleableLoader setUri(Uri uri) {
        super.setUri(uri);
        return this;
    }

    @Override
    public BundleableLoader setSortOrder(String sortOrder) {
        super.setSortOrder(sortOrder);
        return this;
    }

    @Override
    public BundleableLoader setMethod(String method) {
        super.setMethod(method);
        return this;
    }

    @Override
    public BundleableLoader setObserveOnScheduler(Scheduler scheduler) {
        super.setObserveOnScheduler(scheduler);
        return this;
    }

}
