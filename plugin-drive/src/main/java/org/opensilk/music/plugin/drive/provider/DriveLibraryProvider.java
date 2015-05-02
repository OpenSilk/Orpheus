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

import android.content.ComponentName;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.ex.ParcelableException;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.plugin.common.LibraryPreferences;
import org.opensilk.music.plugin.drive.BuildConfig;
import org.opensilk.music.plugin.drive.GlobalComponent;
import org.opensilk.music.plugin.drive.ModelUtil;
import org.opensilk.music.plugin.drive.R;
import org.opensilk.music.plugin.drive.SessionFactory;
import org.opensilk.music.plugin.drive.ui.LibraryChooserActivity;
import org.opensilk.music.plugin.drive.ui.SettingsActivity;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

import static org.opensilk.music.library.LibraryCapability.FOLDERSTRACKS;
import static org.opensilk.music.library.LibraryCapability.SETTINGS;
import static org.opensilk.music.library.LibraryCapability.TRACKS;
import static org.opensilk.music.library.ex.ParcelableException.AUTH_FAILURE;
import static org.opensilk.music.library.ex.ParcelableException.NETWORK;
import static org.opensilk.music.library.ex.ParcelableException.UNKNOWN;
import static org.opensilk.music.plugin.drive.Constants.BASE_FOLDERS_TRACKS_QUERY;
import static org.opensilk.music.plugin.drive.Constants.DEFAULT_ROOT_FOLDER;
import static org.opensilk.music.plugin.drive.Constants.IS_AUDIO;
import static org.opensilk.music.plugin.drive.Constants.LIST_FIELDS;
import static org.opensilk.music.plugin.drive.Constants.TRACKS_QUERY;

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
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .addAbility(FOLDERSTRACKS)
                .addAbility(TRACKS)
                .addAbility(SETTINGS)
                .setPickerComponent(new ComponentName(getContext(), LibraryChooserActivity.class),
                        getContext().getResources().getString(R.string.menu_change_source))
                .setSettingsComponent(new ComponentName(getContext(), SettingsActivity.class),
                        getContext().getResources().getString(R.string.menu_library_settings))
                .setAuthority(AUTHORITY)
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return BuildConfig.APPLICATION_ID;
    }

    @DebugLog
    @Override
    protected void getFoldersTracks(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        final SessionFactory.Session session;
        try {
            session = mSessionFactory.getSession(library);
        } catch (Exception e) {
            handleException(e, subscriber);
            return;
        }

        final String folder = getFolder(library, identity);
        final String q = folder + BASE_FOLDERS_TRACKS_QUERY;

        final FileSubscriber fileSubscriber = new FileSubscriber(session, subscriber, null);

        getFiles(session, q).subscribe(fileSubscriber);
    }

    @Override
    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        final SessionFactory.Session session;
        try {
            session = mSessionFactory.getSession(library);
        } catch (Exception e) {
            handleException(e, subscriber);
            return;
        }

        final String folder = getFolder(library, null);
        final String q = TRACKS_QUERY;

        final FileSubscriber fileSubscriber = new FileSubscriber(session,
                new TracksProxySubscriber(subscriber), null);//TODO new TracksFilterer(folder));

        getFiles(session, q).subscribe(fileSubscriber);
    }

    @Override
    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        final SessionFactory.Session session;
        try {
            session = mSessionFactory.getSession(library);
        } catch (Exception e) {
            handleException(e, subscriber);
            return;
        }

        try {
            File file = session.getDrive().files().get(identity).execute();
            if (!subscriber.isUnsubscribed() && file != null && IS_AUDIO.call(file)) {
                subscriber.onNext(ModelUtil.buildTrack(file, session.getToken()));
                subscriber.onCompleted();
            }
        } catch (Exception e) {
            handleException(e, subscriber);
        }
    }

    String getFolder(String libraryIdentity, String folderIdentity) {
        if (StringUtils.isEmpty(folderIdentity)) {
            String root = mLibraryPrefs.getRootFolder(libraryIdentity);
            if (!StringUtils.isEmpty(root)) {
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
    Observable<Observable<List<File>>> getFiles(
            final SessionFactory.Session driveSession,
            final String query
    ) {
        return Observable.create(new Observable.OnSubscribe<Observable<List<File>>>() {
            @Override
            public void call(Subscriber<? super Observable<List<File>>> subscriber) {
                if (subscriber.isUnsubscribed()) return; //In case of auth fail;
                subscriber.onNext(getPage(subscriber, driveSession, query, null));
            }
        });
    }

    @DebugLog
    Observable<List<File>> getPage(
            final Subscriber<? super Observable<List<File>>> outerSubscriber,
            final SessionFactory.Session driveSession,
            final String query,
            final String paginationToken
    ) {
        return Observable.create(new Observable.OnSubscribe<List<File>>() {
            @Override
            public void call(Subscriber<? super List<File>> subscriber) {
                try {
                    Timber.d("q=%s", query);
                    if (subscriber.isUnsubscribed()) {
                        return; //Shortcircuit if nobody listening
                    }
                    Drive.Files.List req = driveSession.getDrive().files().list()
                            .setQ(query)
                            .setFields(LIST_FIELDS);
                    if (!StringUtils.isEmpty(paginationToken)) {
                        req.setPageToken(paginationToken);
                    }
                    FileList resp = req.execute();
                    Timber.v(ReflectionToStringBuilder.toString(resp, RecursiveToStringStyle.MULTI_LINE_STYLE));
                    //TODO cache response
                    if (!subscriber.isUnsubscribed()) {
                        List<File> files = resp.getItems();
                        if (files != null && !files.isEmpty()) {
                            subscriber.onNext(files);
                        }
                        subscriber.onCompleted();
                    }
                    if (!outerSubscriber.isUnsubscribed()) {
                        if (!StringUtils.isEmpty(resp.getNextPageToken())) {
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

    static void handleException(Throwable e, Subscriber<?> subscriber) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        if (e instanceof GoogleAuthException || e instanceof UserRecoverableAuthIOException) {
            subscriber.onError(new ParcelableException(AUTH_FAILURE, e));
        } else if (e instanceof IOException) {
            subscriber.onError(new ParcelableException(NETWORK, e));
        } else {
            subscriber.onError(new ParcelableException(UNKNOWN, e));
        }
    }

}
