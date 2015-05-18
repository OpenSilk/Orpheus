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

package org.opensilk.music.library.mediastore.provider;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.mediastore.MediaStoreLibraryComponent;
import org.opensilk.music.library.mediastore.R;
import org.opensilk.music.library.mediastore.loader.TracksLoader;
import org.opensilk.music.library.mediastore.ui.StoragePickerActivity;
import org.opensilk.music.library.mediastore.util.CursorHelpers;
import org.opensilk.music.library.mediastore.util.FilesHelper;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.StorageLookup;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Subscriber;
import rx.functions.Action1;
import timber.log.Timber;

import static org.opensilk.music.library.LibraryCapability.DELETE;
import static org.opensilk.music.library.LibraryCapability.FOLDERSTRACKS;
import static org.opensilk.music.library.LibraryCapability.TRACKS;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.appendId;
import static org.opensilk.music.library.provider.LibraryUris.Q.FOLDERS_ONLY;
import static org.opensilk.music.library.provider.LibraryUris.Q.Q;
import static org.opensilk.music.library.provider.LibraryUris.Q.TRACKS_ONLY;

/**
 * Created by drew on 5/17/15.
 */
public class FoldersLibraryProvider extends LibraryProvider {

    @Inject @Named("foldersLibraryBaseAuthority") String mBaseAuthority;
    @Inject StorageLookup mStorageLookup;
    @Inject Provider<TracksLoader> mTracksLoaderProvider;

    @Override
    public boolean onCreate() {
        final AppContextComponent acc;
        if (MediaStoreLibraryProvider.TESTING) {
            Timber.plant(new Timber.DebugTree());
            acc = AppContextComponent.FACTORY.call(getContext());
        } else {
            acc = DaggerService.getDaggerComponent(getContext());
        }
        MediaStoreLibraryComponent.FACTORY.call(acc).inject(this);
        return super.onCreate();
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setCapabilities(FOLDERSTRACKS|TRACKS|DELETE)
                .setPickerComponent(new ComponentName(getContext(), StoragePickerActivity.class),
                        getContext().getString(R.string.folders_picker_title))
                .setAuthority(mAuthority)
                .setLabel(getContext().getString(R.string.folders_library_label))
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return mBaseAuthority;
    }

    @Override
    protected LibraryInfo getDefaultFolder(String library) {
        try {
            return mStorageLookup.getStorageInfo(library);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void browseFolders(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        final File base;
        final File rootDir;
        try {
            base = mStorageLookup.getStorageFile(library);
            rootDir = StringUtils.isEmpty(identity) ? base : new File(base, identity);
            if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                Timber.e("Can't access path %s", rootDir.getPath());
                throw new IllegalArgumentException("Can't access path " + rootDir.getPath());
            }
        } catch (IllegalArgumentException e) {
            subscriber.onError(e);
            return;
        }

        final String q = LibraryExtras.getUri(args).getQueryParameter(Q);
        final boolean dirsOnly = StringUtils.equals(q, FOLDERS_ONLY);
        final boolean tracksOnly = StringUtils.equals(q, TRACKS_ONLY);

        File[] dirList = rootDir.listFiles();
        List<File> files = new ArrayList<>(dirList.length);
        for (File f : dirList) {
            if (!f.canRead()) {
                continue;
            }
            if (f.getName().startsWith(".")) {
                continue;
            }
            if (f.isDirectory() && !tracksOnly) {
                subscriber.onNext(FilesHelper.makeFolder(base, f));
            } else if (f.isFile()) {
                files.add(f);
            }
        }
        //Save ourselves the trouble
        if (dirsOnly) {
            subscriber.onCompleted();
            return;
        } else if (subscriber.isUnsubscribed()) {
            return;
        }
        // convert raw file list into something useful
        List<File> audioFiles = FilesHelper.filterAudioFiles(getContext(), files);
        List<Track> tracks = FilesHelper.convertAudioFilesToTracks(getContext(), base, audioFiles);
        if (subscriber.isUnsubscribed()) {
            return;
        }
        for (Track track : tracks) {
            subscriber.onNext(track);
        }
        subscriber.onCompleted();
    }

    @Override
    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        final File base;
        try {
            base = mStorageLookup.getStorageFile(library);
            if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                Timber.e("Can't access path %s", base.getPath());
                throw new IllegalArgumentException("Can't access path " + base.getPath());
            }
        } catch (IllegalArgumentException e) {
            subscriber.onError(e);
            return;
        }
        TracksLoader l = mTracksLoaderProvider.get();
        l.setSelection(Selections.LOCAL_SONG_PATH);
        l.setSelectionArgs(SelectionArgs.LOCAL_SONG_PATH(base.getAbsolutePath()));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        if (StringUtils.isNumeric(identity)) {
            TracksLoader l = mTracksLoaderProvider.get();
            l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_MEDIA, identity));
            l.createObservable().doOnNext(new Action1<Track>() {
                @Override
                public void call(Track track) {
                    Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
                }
            }).subscribe(subscriber);
        } else {
            final File base;
            try {
                base = mStorageLookup.getStorageFile(library);
                final File f = new File(base, identity);
                if (!f.exists() || !f.isFile() || !f.canRead()) {
                    Timber.e("Can't access file %s", f.getPath());
                    throw new IllegalArgumentException("Can't access file " + f.getPath());
                }
                subscriber.onNext(FilesHelper.makeTrackFromFile(base, f));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }
    }

    @Override
    protected void deleteFolder(String library, String identity, Subscriber<? super Boolean> subscriber, Bundle args) {
        final File base;
        try {
            base = mStorageLookup.getStorageFile(library);
            if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                Timber.e("Can't access path %s", base.getPath());
                throw new IllegalArgumentException("Can't access path " + base.getPath());
            }
        } catch (IllegalArgumentException e) {
            subscriber.onError(e);
            return;
        }
        boolean success = FilesHelper.deleteDirectory(getContext(), new File(base, identity));
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(success);
            subscriber.onCompleted();
        }
    }

    protected void deleteTracks(String library, Subscriber<? super Boolean> subscriber, Bundle args) {
        List<Uri> uris = LibraryExtras.getUriList(args);
        List<Long> ids = new ArrayList<>(uris.size());
        List<String> names = new ArrayList<>(uris.size());
        for (Uri uri : uris) {
            String id = uri.getLastPathSegment();
            if (StringUtils.isNumeric(id)) {
                ids.add(Long.parseLong(id));
            } else {
                names.add(id);
            }
        }
        int numremoved = 0;
        if (!ids.isEmpty()) {
            numremoved += CursorHelpers.deleteTracks(getContext(), ids);
        }
        if (!names.isEmpty()) {
            final File base;
            try {
                base = mStorageLookup.getStorageFile(library);
                if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                    Timber.e("Can't access path %s", base.getPath());
                    throw new IllegalArgumentException("Can't access path " + base.getPath());
                }
            } catch (IllegalArgumentException e) {
                subscriber.onError(e);
                return;
            }
            numremoved += FilesHelper.deleteFiles(base, names);
        }
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(numremoved == uris.size());
            subscriber.onCompleted();
        }
    }
}
