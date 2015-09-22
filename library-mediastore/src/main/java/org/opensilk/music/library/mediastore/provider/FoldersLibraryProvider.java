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

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.mediastore.MediaStoreLibraryComponent;
import org.opensilk.music.library.mediastore.R;
import org.opensilk.music.library.mediastore.loader.TracksLoader;
import org.opensilk.music.library.mediastore.util.CursorHelpers;
import org.opensilk.music.library.mediastore.util.FilesHelper;
import org.opensilk.music.library.mediastore.util.StorageLookup;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Folder;
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

import static org.opensilk.music.library.mediastore.util.CursorHelpers.appendId;

/**
 * Created by drew on 5/17/15.
 */
public class FoldersLibraryProvider extends LibraryProvider {

    @Inject @Named("foldersLibraryBaseAuthority") String mBaseAuthority;
    @Inject StorageLookup mStorageLookup;
    @Inject Provider<TracksLoader> mTracksLoaderProvider;

    UriMatcher mUriMatcher;

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
        super.onCreate();
        mUriMatcher = FoldersUris.makeMatcher(mAuthority);
        return true;
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLabel(getContext().getString(R.string.folders_library_label))
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return mBaseAuthority;
    }

    @Override
    protected void listObjs(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        switch (mUriMatcher.match(uri)) {
            case FoldersUris.M_FOLDERS: {
                browseFolders(uri.getPathSegments().get(0), null, subscriber, args);
                break;
            }
            case FoldersUris.M_FOLDER: {
                browseFolders(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                break;
            }
            default:
                Timber.w("Unmatched uri %s", uri);
                subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                        new IllegalArgumentException("Invalid uri " + uri)));
        }
    }

    @Override
    protected void scanObjs(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        args.putBoolean("fldr.isscan", true);
        listObjs(uri, subscriber, args);
    }

    @Override
    protected void getObj(Uri uri, Subscriber<? super Bundleable> subscriber, Bundle args) {
        switch (mUriMatcher.match(uri)) {
            case FoldersUris.M_FOLDER: {
                browseFolders(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                break;
            }
            case FoldersUris.M_TRACK_MS:
            case FoldersUris.M_TRACK_PTH: {
                getTrack(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                break;
            }
            default: {
                Timber.w("Unmatched uri %s", uri);
                subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                        new IllegalArgumentException("Invalid uri " + uri)));
            }
        }
    }

    @Override
    protected void listRoots(Uri uri, Subscriber<? super Container> subscriber, Bundle args) {
        List<StorageLookup.StorageVolume> volumes = mStorageLookup.getStorageVolumes();
        if (volumes == null || volumes.size() == 0) {
            subscriber.onCompleted();
            return;
        }
        for (StorageLookup.StorageVolume v : volumes) {
            Folder f = Folder.builder()
                    .setUri(FoldersUris.folders(mAuthority, String.valueOf(v.id)))
                    .setParentUri(LibraryUris.rootUri(mAuthority))
                    .setName(v.description)
                    .build();
            subscriber.onNext(f);
        }
        subscriber.onCompleted();
    }

    void browseFolders(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        final StorageLookup.StorageVolume volume;
        final File rootDir;
        try {
            volume = mStorageLookup.getStorageVolume(library);
            final File base = new File(volume.path);
            rootDir = StringUtils.isEmpty(identity) ? base : new File(base, identity);
            if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                Timber.e("Can't access path %s", rootDir.getPath());
                throw new IllegalArgumentException("Can't access path " + rootDir.getPath());
            }
        } catch (IllegalArgumentException e) {
            subscriber.onError(e);
            return;
        }

        final boolean isScan = args.getBoolean("fldr.isscan", false);

        File[] dirList = rootDir.listFiles();
        List<File> files = new ArrayList<>(dirList.length);
        for (File f : dirList) {
            if (!f.canRead()) {
                continue;
            }
            if (f.getName().startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                subscriber.onNext(FilesHelper.makeFolder(mAuthority, volume, f));
            } else if (f.isFile()) {
                files.add(f);
            }
        }
        //Save ourselves the trouble
        if (subscriber.isUnsubscribed()) {
            return;
        }
        // convert raw file list into something useful
        List<File> audioFiles = FilesHelper.filterAudioFiles(getContext(), files);
        List<Track> tracks;
        if (isScan) {
            //We want orpheus to extract the meta
            tracks = FilesHelper.convertAudioFilesToTracksMinimal(getContext(),
                    mAuthority, volume, audioFiles);
        } else {
            //We pull some meta from the medastore to make the display prettier
            tracks = FilesHelper.convertAudioFilesToTracks(getContext(),
                    mAuthority, volume, audioFiles);
        }
        if (subscriber.isUnsubscribed()) {
            return;
        }
        for (Track track : tracks) {
            subscriber.onNext(track);
        }
        subscriber.onCompleted();
    }

    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        if (StringUtils.isNumeric(identity)) {
            TracksLoader l = mTracksLoaderProvider.get();
            l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_MEDIA, identity));
            l.createObservable().doOnNext(new Action1<Track>() {
                @Override
                public void call(Track track) {
                    Timber.v("Track name=%s artist=%s albumArtist=%s", track.getDisplayName(), track.getArtistName(), track.getAlbumArtistName());
                }
            }).subscribe(subscriber);
        } else {
            try {
                final StorageLookup.StorageVolume volume = mStorageLookup.getStorageVolume(library);
                final File base = new File(volume.path);
                final File f = new File(base, identity);
                if (!f.exists() || !f.isFile() || !f.canRead()) {
                    Timber.e("Can't access file %s", f.getPath());
                    throw new IllegalArgumentException("Can't access file " + f.getPath());
                }
                subscriber.onNext(FilesHelper.makeTrackFromFile(mAuthority, volume, f));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }
    }

    @Override
    protected void deleteObj(Uri uri, Subscriber<? super Uri> subscriber, Bundle args) {
        final File base;
        try {
            base = mStorageLookup.getStorageFile(uri.getPathSegments().get(0));
            if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                Timber.e("Can't access path %s", base.getPath());
                throw new IllegalArgumentException("Can't access path " + base.getPath());
            }
        } catch (IllegalArgumentException e) {
            subscriber.onError(e);
            return;
        }
//        boolean success = FilesHelper.deleteDirectory(getContext(), new File(base, identity));
//        if (!subscriber.isUnsubscribed()) {
//            subscriber.onNext(success);
//            subscriber.onCompleted();
//        }
        subscriber.onError(new UnsupportedOperationException());
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
