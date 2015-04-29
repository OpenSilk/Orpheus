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

import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.plugin.common.LibraryPreferences;
import org.opensilk.music.plugin.drive.BuildConfig;
import org.opensilk.music.plugin.drive.GlobalComponent;
import org.opensilk.music.plugin.drive.ModelUtil;
import org.opensilk.music.plugin.drive.SessionFactory;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

import static org.opensilk.music.api.exception.ParcelableException.AUTH_FAILURE;
import static org.opensilk.music.api.exception.ParcelableException.NETWORK;
import static org.opensilk.music.plugin.drive.Constants.AUDIO_MIME_WILDCARD;
import static org.opensilk.music.plugin.drive.Constants.AUDIO_OGG_MIMETYPE;
import static org.opensilk.music.plugin.drive.Constants.BASE_QUERY;
import static org.opensilk.music.plugin.drive.Constants.DEFAULT_ROOT_FOLDER;
import static org.opensilk.music.plugin.drive.Constants.FIELDS;
import static org.opensilk.music.plugin.drive.Constants.FOLDER_MIMETYPE;
import static org.opensilk.music.plugin.drive.Constants.FOLDER_SONG_QUERY;

/**
 * Created by drew on 4/28/15.
 */
public class DriveLibraryProvider extends LibraryProvider {

    public static final String AUTHORITY = AUTHORITY_PFX+BuildConfig.APPLICATION_ID;

    @Inject SessionFactory mSessionFactory;
    @Inject LibraryPreferences mLibraryPrefs;

    @Override
    public boolean onCreate() {
        DaggerService.<GlobalComponent>getDaggerComponent(getContext()).inject(this);
        return super.onCreate();
    }

    @Override
    protected String getAuthority() {
        return BuildConfig.APPLICATION_ID;
    }

    @DebugLog
    @Override
    protected void getFoldersTracks(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        final SessionFactory.Session session = mSessionFactory.getSession(library);

        final String folder = getFolder(library, identity);
        final String q = folder + BASE_QUERY + " and" + FOLDER_SONG_QUERY;
        final FileSubscriber fileSubscriber = new FileSubscriber(session, subscriber);
        getFiles(session, q).subscribe(fileSubscriber);
    }

    String getFolder(String libraryIdentity, String folderIdentity) {
        if (TextUtils.isEmpty(folderIdentity)) {
            String root = mLibraryPrefs.getRootFolder(libraryIdentity);
            if (!TextUtils.isEmpty(root)) {
                // use preferred root
                return "'"+root+"'";
            } else {
                // use real root
                return "'"+DEFAULT_ROOT_FOLDER+"'";
            }
        } else {
            return "'"+folderIdentity+"'";
        }
    }

    @DebugLog
    Observable<Observable<List<File>>> getFiles(final SessionFactory.Session driveSession, final String query) {
        return Observable.create(new Observable.OnSubscribe<Observable<List<File>>>() {
            @Override
            public void call(Subscriber<? super Observable<List<File>>> subscriber) {
                if (subscriber.isUnsubscribed()) return; //In case of auth fail;
                subscriber.onNext(getPage(subscriber, driveSession, query, null));
            }
        });
    }

    @DebugLog
    Observable<List<File>> getPage(final Subscriber<? super Observable<List<File>>> outerSubscriber,
                             final SessionFactory.Session driveSession,
                             final String query,
                             final String paginationToken) {
        return Observable.create(new Observable.OnSubscribe<List<File>>() {
            @Override
            public void call(Subscriber<? super List<File>> subscriber) {
                try {
                    Timber.d("q=" + query);
                    if (subscriber.isUnsubscribed()) {
                        return; //Shortcircuit if nobody listening
                    }
                    Drive.Files.List req = driveSession.getDrive().files().list()
                            .setQ(query)
                            .setFields(FIELDS);
                    if (!TextUtils.isEmpty(paginationToken)) {
                        req.setPageToken(paginationToken);
                    }
                    FileList resp = req.execute();
                    //Timber.w(ReflectionToStringBuilder.toString(resp, RecursiveToStringStyle.MULTI_LINE_STYLE));
                    //TODO cache response
                    if (!subscriber.isUnsubscribed()) {
                        List<File> files = resp.getItems();
                        if (files != null && !files.isEmpty()) {
                            subscriber.onNext(files);
                        }
                        subscriber.onCompleted();
                    }
                    if (!outerSubscriber.isUnsubscribed()) {
                        if (!TextUtils.isEmpty(resp.getNextPageToken())) {
                            outerSubscriber.onNext(getPage(outerSubscriber, driveSession, query, resp.getNextPageToken()));
                        } else {
                            outerSubscriber.onCompleted();
                        }
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    class FileSubscriber extends Subscriber<Observable<List<File>>> {

        final SessionFactory.Session driveSession;
        final Subscriber<? super Bundleable> wrapped;

        String authToken = null;

        public FileSubscriber(SessionFactory.Session driveSession, Subscriber<? super Bundleable> wrapped) {
            this.driveSession = driveSession;
            this.wrapped = wrapped;
        }

        @Override
        @DebugLog
        public void onStart() {
            super.onStart();
            //TODO not the best place for this. need to push it up higher. and cache in Session
            try {
                authToken = driveSession.getCredential().getToken();
            } catch (Exception e) {
                unsubscribe();
                onError(e);
            }
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
            if (e instanceof GoogleAuthException) {
                wrapped.onError(new ParcelableException(AUTH_FAILURE,e));
            } else if (e instanceof IOException) {
                wrapped.onError(new ParcelableException(NETWORK, e));
            } else {
                wrapped.onError(new ParcelableException(e));
            }
            wrapped.onError(e);
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
                        final String mime = f.getMimeType();
                        Bundleable b = null;
                        if (TextUtils.equals(FOLDER_MIMETYPE, mime)) {
                            try {
                                b = ModelUtil.buildFolder(f);
                            } catch (Exception e) {
                                Timber.w(e, "Error transforming File to Folder");
                            }
                            //Extra precaution
                        } else if (mime.contains(AUDIO_MIME_WILDCARD)
                                || TextUtils.equals(mime, AUDIO_OGG_MIMETYPE)) {
                            try {
                                b = ModelUtil.buildTrack(f, authToken);
                            } catch (Exception e) {
                                Timber.w(e, "Error transforming File to Track");
                            }
                        }
                        Timber.d("Bundleable=%s", b);
                        if (b != null) {
                            wrapped.onNext(b);
                        }
                    }
                }
            }));
        }
    }
}
