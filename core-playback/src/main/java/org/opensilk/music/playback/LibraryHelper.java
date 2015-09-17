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

package org.opensilk.music.playback;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 5/8/15.
 */
public class LibraryHelper {
    final Context context;

    @Inject
    public LibraryHelper(@ForApplication Context context) {
        this.context = context;
    }

    public Track getTrack(Uri uri) {
        try {
            return new BundleableLoader(context, uri, null)
                    .createObservable().flatMap(new Func1<List<Bundleable>, Observable<? extends Bundleable>>() {
                        @Override
                        public Observable<? extends Bundleable> call(List<Bundleable> bundleables) {
                            return Observable.from(bundleables);
                        }
                    }).cast(Track.class).toBlocking().first();
        } catch (Exception e) {
            Timber.e(e, "getTrack");
            return null;
        }
    }

    public List<Uri> getTracks(final Uri uri, String sortorder) {
//        try {
//            final String authority = uri.getAuthority();
//            final String library = uri.getPathSegments().get(0);
//            return new BundleableLoader(context, uri, sortorder)
//                    .createObservable().flatMap(new Func1<List<Bundleable>, Observable<Bundleable>>() {
//                        @Override
//                        public Observable<Bundleable> call(List<Bundleable> bundleables) {
//                            return Observable.from(bundleables);
//                        }
//                    }).collect(new ArrayList<Uri>(), new Action2<ArrayList<Uri>, Bundleable>() {
//                        @Override
//                        public void call(ArrayList<Uri> uris, Bundleable bundleable) {
//                            uris.add(LibraryUris.track(authority, library, bundleable.getIdentity()));
//                        }
//                    }).toBlocking().first();
//        } catch (Exception e) {
//            Timber.e(e, "getTracks");
//            return Collections.emptyList();
//        }
        return Collections.emptyList();
    }

}
