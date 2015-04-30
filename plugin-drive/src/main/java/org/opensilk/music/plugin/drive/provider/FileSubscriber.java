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

package org.opensilk.music.plugin.drive.provider;

import com.google.api.services.drive.model.File;

import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.plugin.drive.ModelUtil;
import org.opensilk.music.plugin.drive.SessionFactory;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.music.plugin.drive.Constants.IS_AUDIO;
import static org.opensilk.music.plugin.drive.Constants.IS_FOLDER;

/**
 * Handles recursively querying drive and transforming Files into Bundleables
 *
 * Created by drew on 4/29/15.
 */
class FileSubscriber extends Subscriber<Observable<List<File>>> {

    final SessionFactory.Session driveSession;
    final Subscriber<? super Bundleable> wrapped;
    final Func1<File, Boolean> filterFunc;

    public FileSubscriber(
            SessionFactory.Session driveSession,
            Subscriber<? super Bundleable> wrapped,
            Func1<File, Boolean> filterFunc
    ) {
        this.driveSession = driveSession;
        this.wrapped = wrapped;
        this.filterFunc = filterFunc;
    }

    @Override
    public void onStart() {
        super.onStart();
        //ensure we get unsubscribed if they are
        wrapped.add(this);
    }

    @Override
    public void onCompleted() {
        Timber.v("onCompleted(outer)");
        wrapped.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        Timber.e(e, "onError(outer)");
        DriveLibraryProvider.handleException(e, wrapped);
    }

    @Override
    public void onNext(Observable<List<File>> fileObservable) {
        add(fileObservable.subscribe(new Subscriber<List<File>>() {
            @Override
            public void onCompleted() {
                Timber.v("onCompleted(inner)");
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("onError(inner)");
                FileSubscriber.this.onError(e);
            }

            @Override
            //@DebugLog
            public void onNext(List<File> files) {
                for (File f : files) {
                    if (filterFunc == null || filterFunc.call(f)) {
                        Bundleable b = null;
                        if (IS_FOLDER.call(f)) {
                            try {
                                b = ModelUtil.buildFolder(f);
                            } catch (Exception e) {
                                Timber.w(e, "Error transforming File to Folder");
                            }
                        } else if (IS_AUDIO.call(f)) {
                            try {
                                b = ModelUtil.buildTrack(f, driveSession.getToken());
                            } catch (Exception e) {
                                Timber.w(e, "Error transforming File to Track");
                            }
                        } else {
                            Timber.w("Unknown file type %s. Is the query right?", f.getMimeType());
                        }
                        if (b != null) {
                            wrapped.onNext(b);
                        }
                    }
                }
            }
        }));
    }
}
