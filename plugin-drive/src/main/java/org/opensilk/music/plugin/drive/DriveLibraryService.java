/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.drive;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.RemoteLibraryService;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.plugin.common.LibraryPreferences;
import org.opensilk.music.plugin.drive.ui.LibraryChooserActivity;
import org.opensilk.music.plugin.drive.ui.SettingsActivity;
import org.opensilk.music.plugin.drive.util.Helpers;
import org.opensilk.music.plugin.drive.util.RequestCache;
import org.opensilk.music.plugin.drive.util.DriveHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import static org.opensilk.music.plugin.drive.Constants.*;
import static org.opensilk.music.api.exception.ParcelableException.NETWORK;
import static org.opensilk.music.api.exception.ParcelableException.AUTH_FAILURE;

/**
 * Created by drew on 6/13/14.
 */
public class DriveLibraryService extends RemoteLibraryService {

    @Inject DriveHelper mDriveHelper;
    @Inject LibraryPreferences mLibraryPrefs;
    @Inject RequestCache mCache;

    final List<FileSubscriber> activeSubscribers = Collections.synchronizedList(new ArrayList<FileSubscriber>(4));

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDriveHelper.destroy();
    }

    /*
     * Abstract methods
     */

    @Override
    protected PluginConfig getConfig() {
        return new PluginConfig.Builder()
                .addAbility(PluginConfig.SEARCHABLE)
                .setPickerComponent(new ComponentName(this, LibraryChooserActivity.class),
                        getResources().getString(R.string.menu_change_source))
                .setSettingsComponent(new ComponentName(this, SettingsActivity.class),
                        getResources().getString(R.string.menu_library_settings))
                .build();
    }

    @Override
    protected void browseFolders(String libraryIdentity, String folderIdentity, final int maxResults, Bundle paginationBundle, final Result callback) {
        final DriveHelper.Session session = mDriveHelper.getSession(libraryIdentity);
        final String folder = getFolder(libraryIdentity, folderIdentity);
        final int startpos = (paginationBundle != null) ? paginationBundle.getInt("startpos") : 0;
        final String q = folder + BASE_QUERY + " and" + FOLDER_SONG_QUERY;
        final String cacheKey = "browse" + folder;

        if (requestInflight(cacheKey, callback)) return;

        if (mCache.get(cacheKey, startpos, maxResults, callback)) return;

        final FileSubscriber subscriber = new FileSubscriber(session, maxResults, false, cacheKey, callback);
        activeSubscribers.add(subscriber);
        getFiles(session, q).subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    protected void listSongsInFolder(String libraryIdentity, String folderIdentity, int maxResults, Bundle paginationBundle, Result callback) {
        final DriveHelper.Session session = mDriveHelper.getSession(libraryIdentity);
        final String folder = getFolder(libraryIdentity, folderIdentity);
        final int startpos = (paginationBundle != null) ? paginationBundle.getInt("startpos") : 0;
        final String q = folder + BASE_QUERY + " and" + SONG_QUERY;
        final String cacheKey = "listSongs" + folder;

        if (requestInflight(cacheKey, callback)) return;

        if (mCache.get(cacheKey, startpos, maxResults, callback)) return;

        final FileSubscriber subscriber = new FileSubscriber(session, maxResults, true, cacheKey, callback);
        activeSubscribers.add(subscriber);
        getFiles(session, q).subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    protected void search(String libraryIdentity, String query, int maxResults, Bundle paginationBundle, Result callback) {
        final DriveHelper.Session session = mDriveHelper.getSession(libraryIdentity);
        final int startpos = (paginationBundle != null) ? paginationBundle.getInt("startpos") : 0;
        //TODO sanitize query
        final String q = "title contains '"+query+"' and trashed=false and" + FOLDER_SONG_QUERY;

        if (requestInflight(q, callback)) return;

        if (mCache.get(q, startpos, maxResults, callback)) return;

        final FileSubscriber subscriber = new FileSubscriber(session, maxResults, false, q, callback);
        activeSubscribers.add(subscriber);
        getFiles(session, q).subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    static Bundle dematerializeFile(File f, String authToken) {
        final String mime = f.getMimeType();
        if (TextUtils.equals(FOLDER_MIMETYPE, mime)) {
            return Helpers.buildFolder(f).toBundle();
        } else if (mime.contains("audio") || TextUtils.equals(mime, "application/ogg")) {
            return Helpers.buildSong(f, authToken).toBundle();
        } else {
            throw new IllegalArgumentException("File is wrong type: " + mime);
        }
    }

    Observable<Bundle> doBrowse(final DriveHelper.Session session, final String q) {
        return Observable.create(new Observable.OnSubscribe<Bundle>() {
            @Override
            public void call(Subscriber<? super Bundle> subscriber) {
                try {
                    Timber.d("q=" + q);
                    final String authToken = session.getCredential().getToken();
                    String pageToken = null;
                    do {
                        Drive.Files.List req = session.getDrive().files().list()
                                .setQ(q)
                                .setFields(FIELDS)
                                .setMaxResults(500); //More the better
                        if (!TextUtils.isEmpty(pageToken)) req.setPageToken(pageToken);
                        FileList resp = req.execute();
                        List<File> files = resp.getItems();
                        for (File f : files) {
                            if (subscriber.isUnsubscribed()) return;
                            subscriber.onNext(dematerializeFile(f, authToken));
                        }
                        pageToken = resp.getNextPageToken();
                    } while (!TextUtils.isEmpty(pageToken));
                    if (!subscriber.isUnsubscribed())
                        subscriber.onCompleted();
                } catch (IOException e) {
                    if (!subscriber.isUnsubscribed())
                        subscriber.onError(new ParcelableException(NETWORK, e));
                } catch (GoogleAuthException e) {
                    if (!subscriber.isUnsubscribed())
                        subscriber.onError(new ParcelableException(AUTH_FAILURE, e));
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed())
                        subscriber.onError(e);
                }
            }
        });
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

    boolean requestInflight(String cacheKey, Result callback) {
        synchronized (activeSubscribers) {
            for (FileSubscriber s : activeSubscribers) {
                if (s.cacheKey.equals(cacheKey)) {
                    s.addListener(callback);
                    return true;
                }
            }
            return false;
        }
    }

    // Drive api wont let us sort so we fetch everything at once
    // sort it ourselves and cache the result so pagination works
    // as Orpheus expects it to
    Observable<Observable<File>> getFiles(final DriveHelper.Session driveSession,
                                          final String query) {
        return Observable.create(new Observable.OnSubscribe<Observable<File>>() {
            @Override
            public void call(Subscriber<? super Observable<File>> subscriber) {
                if (subscriber.isUnsubscribed()) return; //In case of auth fail;
                subscriber.onNext(getPage(subscriber, driveSession, query, null));
            }
        });
    }

    Observable<File> getPage(final Subscriber<? super Observable<File>> outerSubscriber,
                             final DriveHelper.Session driveSession,
                             final String query,
                             final String paginationToken) {
        return Observable.create(new Observable.OnSubscribe<File>() {
            @Override
            public void call(Subscriber<? super File> subscriber) {
                try {
                    Timber.d("q=" + query);
                    Drive.Files.List req = driveSession.getDrive().files().list()
                            .setQ(query)
                            .setFields(FIELDS)
                            .setMaxResults(500); //More the better
                    if (!TextUtils.isEmpty(paginationToken)) req.setPageToken(paginationToken);
                    FileList resp = req.execute();
                    List<File> files = resp.getItems();
                    for (File f : files) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(f);
                    }
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onCompleted();
                    if (!TextUtils.isEmpty(resp.getNextPageToken())) {
                        if (!outerSubscriber.isUnsubscribed())
                            outerSubscriber.onNext(
                                    getPage(outerSubscriber, driveSession, query, resp.getNextPageToken()
                            ));
                    } else {
                        if (!outerSubscriber.isUnsubscribed()) outerSubscriber.onCompleted();
                    }
                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed()) subscriber.onError(e);
                }
            }
        });
    }

    class FileSubscriber extends Subscriber<Observable<File>> {

        final DriveHelper.Session driveSession;
        final int maxResults;
        final boolean songsOnly;
        final String cacheKey;

        final List<Result> callbacks = new ArrayList<>(2);
        final List<Folder> folders = new ArrayList<>(100);
        final List<Song> songs = new ArrayList<>(100);
        final List<Bundle> bundlesCache = new ArrayList<>(100);
        final List<Bundle> bundlesResult = new ArrayList<>(30);

        String authToken = null;

        FileSubscriber(DriveHelper.Session driveSession,
                       int maxResults,
                       boolean songsOnly,
                       String cacheKey,
                       Result callback) {
            this.driveSession = driveSession;
            this.maxResults = maxResults;
            this.songsOnly = songsOnly;
            this.cacheKey = cacheKey;
            addListener(callback);
        }

        void addListener(Result result) {
            synchronized (callbacks) {
                callbacks.add(result);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            try {
                authToken = driveSession.getCredential().getToken();
            } catch (Exception e) {
                unsubscribe();
                onError(e);
            }
        }

        @Override
        public void onCompleted() {
            Timber.v("onCompleted(outer)");
            if (!songsOnly && !folders.isEmpty()) {
                // Sort
                Collections.sort(folders, new Comparator<Folder>() {
                    @Override
                    public int compare(Folder lhs, Folder rhs) {
                        return lhs.name.compareTo(rhs.name);
                    }
                });
                for (Folder folder : folders) {
                    // transform
                    Bundle b = folder.toBundle();
                    bundlesCache.add(b);
                    // populate initial results
                    if (bundlesResult.size() < maxResults) {
                        bundlesResult.add(b);
                    }
                }
            }

            if (!songs.isEmpty()) {
                // sort
                Collections.sort(songs, new Comparator<Song>() {
                    @Override
                    public int compare(Song lhs, Song rhs) {
                        return lhs.name.compareTo(rhs.name);
                    }
                });
                for (Song song : songs) {
                    // transform
                    Bundle b = song.toBundle();
                    bundlesCache.add(b);
                    // populate initial results
                    if (bundlesResult.size() < maxResults) {
                        bundlesResult.add(b);
                    }
                }
            }

            // cache
            mCache.put(cacheKey, bundlesCache);

            // if cache is larger than initial results add page token
            Bundle token = null;
            if (bundlesResult.size() < bundlesCache.size()) {
                token = new Bundle(1);
                Timber.d("onCompleted() maxresults=%d, resultsize=%d, cacheSize=%d",
                        maxResults, bundlesResult.size(), bundlesCache.size());
                token.putInt("startpos", bundlesResult.size());
            }

            // iterate the listeners, really we should only
            // have one, but the api doesnt yet allow canceling
            // so we could end up with multiple even though
            // only the last will be valid
            synchronized (callbacks) {
                for (Result callback : callbacks) {
                    try {
                        callback.onNext(bundlesResult, token);
                    } catch (RemoteException ignored) {}
                }
            }

            // remove ourselves so no additional callbacks will be added
            activeSubscribers.remove(this);
        }

        @Override
        public void onError(Throwable e) {
            Timber.e(e, "onError(inner)");
            synchronized (callbacks) {
                for (Result callback : callbacks) {
                    if (e instanceof GoogleAuthException) {
                        try {
                            callback.onError(new ParcelableException(AUTH_FAILURE,e));
                        } catch (RemoteException ignored) { }
                    } else if (e instanceof IOException) {
                        try {
                            callback.onError(new ParcelableException(NETWORK,e));
                        } catch (RemoteException ignored) { }
                    } else {
                        try {
                            callback.onError(new ParcelableException(e));
                        } catch (RemoteException ignored) { }
                    }
                }
            }
            activeSubscribers.remove(this);
        }

        @Override
        public void onNext(Observable<File> fileObservable) {
            add(fileObservable.subscribe(new Subscriber<File>() {
                @Override
                public void onCompleted() {
                    Timber.v("onCompleted(inner)");
                }

                @Override
                public void onError(Throwable e) {
                    FileSubscriber.this.onError(e);
                }

                @Override
                public void onNext(File file) {
                    final String mime = file.getMimeType();
                    if (TextUtils.equals(FOLDER_MIMETYPE, mime)) {
                        try {
                            folders.add(Helpers.buildFolder(file));
                        } catch (Exception e) {
                            unsubscribe();
                            onError(e);
                        }
                    } else if (mime.contains("audio")
                            || TextUtils.equals(mime, "application/ogg")) { //TODO more mimes?
                        try {
                            songs.add(Helpers.buildSong(file, authToken));
                        } catch (Exception e) {
                            unsubscribe();
                            onError(e);
                        }
                    }
                }
            }));
        }
    }

    /**
     *
     */
    static class ListFilesRunner implements Runnable {
        private final DriveHelper.Session driveSession;
        private final int maxResults;
        private final String query;
        private final String paginationToken;
        private final boolean songsOnly;
        private final Result callback;

        ListFilesRunner(DriveHelper.Session driveSession, int maxResults, String query,
                        String paginationToken, boolean songsOnly, Result callback) {
            this.driveSession = driveSession;
            this.maxResults = maxResults;
            this.query = query;
            this.paginationToken = paginationToken;
            this.songsOnly = songsOnly;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                Timber.d("q=" + query);
                Drive.Files.List req = driveSession.getDrive().files().list()
                        .setQ(query)
                        .setFields(FIELDS)
                        .setMaxResults(maxResults);
                if (!TextUtils.isEmpty(paginationToken)) {
                    req.setPageToken(paginationToken);
                }
                FileList resp = req.execute();
                List<File> files = resp.getItems();
                List<Folder> folders = new ArrayList<>();
                List<Song> songs = new ArrayList<>();
                final String authToken = driveSession.getCredential().getToken();
                for (File f : files) {
                    final String mime = f.getMimeType();
                    if (TextUtils.equals(FOLDER_MIMETYPE, mime)) {
                        if (!songsOnly) {
                            Folder folder = Helpers.buildFolder(f);
                            folders.add(folder);
                        }
                    } else if (mime.contains("audio") || TextUtils.equals(mime, "application/ogg")) {
                        Song song = Helpers.buildSong(f, authToken);
                        songs.add(song);
                    }
                }

                // Combine results into single list
                final List<Bundle> resources = new ArrayList<>((songsOnly ? 0 : folders.size()) + songs.size());
                if (!songsOnly) {
                    for (Folder f : folders) {
                        resources.add(f.toBundle());
                    }
                }
                for (Song s : songs) {
                    resources.add(s.toBundle());
                }

                final Bundle b;
                if (!TextUtils.isEmpty(resp.getNextPageToken())) {
                    b = new Bundle(1);
                    b.putString("token", resp.getNextPageToken());
                } else {
                    b = null;
                }
                try {
                    callback.onNext(resources, b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    callback.onError(new ParcelableException(NETWORK,e));
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            } catch (GoogleAuthException e) {
                e.printStackTrace();
                try {
                    callback.onError(new ParcelableException(AUTH_FAILURE,e));
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    protected void querySongs(String libraryIdentity, final int maxResults, Bundle paginationBundle, final Result callback) throws RemoteException {
        final DriveHelper.Session session = mDriveHelper.getSession(libraryIdentity);
        final String paginationToken;
        if (paginationBundle != null) {
            paginationToken = paginationBundle.getString("token");
        } else {
            paginationToken = null;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Drive.Files.List req = session.getDrive().files().list()
                            .setQ("trashed=false and (mimeType contains 'audio' or mimeType = 'application/ogg')")
                            .setFields(FIELDS)
                            .setMaxResults(maxResults);
                    if (!TextUtils.isEmpty(paginationToken)) {
                        req.setPageToken(paginationToken);
                    }
                    FileList resp = req.execute();
                    List<File> files = resp.getItems();
                    List<Bundle> songs = new ArrayList<>();
                    final String authToken = session.getCredential().getToken();
                    for (File f : files) {
                        final String mime = f.getMimeType();
                        if (mime.contains("audio") || TextUtils.equals(mime, "application/ogg")) {
                            Song song = Helpers.buildSong(f, authToken);
                            songs.add(song.toBundle());
                        }
                    }
                    final Bundle b;
                    if (!TextUtils.isEmpty(resp.getNextPageToken())) {
                        b = new Bundle(1);
                        b.putString("token", resp.getNextPageToken());
                    } else {
                        b = null;
                    }
                    try {
                        callback.onNext(songs, b);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } catch (IOException|GoogleAuthException e) {
                    e.printStackTrace();
                    try {
                        callback.onError(new ParcelableException(e));
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(r);
    }

}
