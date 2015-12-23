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

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.mediastore.R;
import org.opensilk.music.library.mediastore.util.FilesHelper;
import org.opensilk.music.library.mediastore.util.PlaylistUtil;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.playlist.PlaylistOperationListener;
import org.opensilk.music.library.playlist.provider.PlaylistLibraryProvider;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Created by drew on 5/17/15.
 */
public class FoldersLibraryProvider extends PlaylistLibraryProvider {

    @Inject @Named("foldersLibraryAuthority") String mAuthority;
    @Inject StorageLookup mStorageLookup;

    UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        final AppContextComponent acc = DaggerService.getDaggerComponent(getContext());
        FoldersLibraryComponent.FACTORY.call(acc).inject(this);
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
    protected String getAuthority() {
        return mAuthority;
    }

    @Override
    @DebugLog
    protected Observable<Model> getListObjsObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                switch (mUriMatcher.match(uri)) {
                    case FoldersUris.M_FOLDERS: {
                        browseFolders(uri.getPathSegments().get(0), null, subscriber, args);
                        return;
                    }
                    case FoldersUris.M_FOLDER: {
                        browseFolders(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                        return;
                    }
                    case FoldersUris.M_PLAYLISTS: {
                        final List<Playlist> playlists = PlaylistUtil.getPlaylists(getContext(), mAuthority);
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        if (playlists == null) {
                            subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                            return;
                        }
                        for (Playlist p : playlists) {
                            subscriber.onNext(p);
                        }
                        subscriber.onCompleted();
                        return;
                    }
                    case FoldersUris.M_PLAYLIST_TRACKS: {
                        final List<String> segs = uri.getPathSegments();
                        final String plst = segs.get(segs.size() - 2);
                        final List<StorageLookup.StorageVolume> volumes = mStorageLookup.getStorageVolumes();
                        final List<Track> tracks = PlaylistUtil.getPlaylistMembers(getContext(), plst, mAuthority, volumes);
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        if (tracks == null) {
                            subscriber.onError(new NullPointerException("Unable to query playlist"));
                            return;
                        }
                        for (Track t : tracks) {
                            subscriber.onNext(t);
                        }
                        subscriber.onCompleted();
                        return;
                    }
                    default:
                        Timber.w("Unmatched uri %s", uri);
                        subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                                new IllegalArgumentException("Invalid uri " + uri)));
                }
            }
        });
    }

    @Override
    protected Observable<Model> getGetObjObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                switch (mUriMatcher.match(uri)) {
                    case FoldersUris.M_FOLDERS: {
                        final String library = uri.getPathSegments().get(0);
                        final StorageLookup.StorageVolume volume = getStorageVolume(library);
                        if (volume == null) {
                            subscriber.onError(new IllegalArgumentException("Can't access volume " + library));
                            return;
                        }
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(FilesHelper.makeRoot(mAuthority, volume));
                        subscriber.onCompleted();
                        return;
                    }
                    case FoldersUris.M_FOLDER: {
                        final String library = uri.getPathSegments().get(0);
                        final StorageLookup.StorageVolume volume = getStorageVolume(library);
                        if (volume == null) {
                            subscriber.onError(new IllegalArgumentException("Can't access volume " + library));
                            return;
                        }
                        final String identity = uri.getLastPathSegment();
                        final File rootDir = new File(volume.path, identity);
                        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                            subscriber.onError(new IllegalArgumentException("Can't access path " + rootDir.getPath()));
                            return;
                        }
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        subscriber.onNext(FilesHelper.makeFolder(mAuthority, volume, rootDir));
                        subscriber.onCompleted();
                        return;
                    }
                    case FoldersUris.M_TRACK_MS:
                    case FoldersUris.M_TRACK_PTH: {
                        getTrack(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                        return;
                    }
                    case FoldersUris.M_PLAYLIST: {
                        final Playlist playlist = PlaylistUtil.getPlaylist(getContext(), mAuthority, uri.getLastPathSegment());
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }
                        if (playlist == null) {
                            subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                            return;
                        }
                        subscriber.onNext(playlist);
                        subscriber.onCompleted();
                        return;
                    }
                    default: {
                        Timber.w("Unmatched uri %s", uri);
                        subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                                new IllegalArgumentException("Invalid uri " + uri)));
                    }
                }
            }
        });
    }

    @Override
    protected Observable<Container> getListRootsObservable(Uri uri, Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Container>() {
            @Override
            public void call(Subscriber<? super Container> subscriber) {
                List<StorageLookup.StorageVolume> volumes = mStorageLookup.getStorageVolumes();
                if (volumes == null || volumes.size() == 0) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(new Exception("No storage volumes found"));
                    }
                } else if (!subscriber.isUnsubscribed()) {
                    for (StorageLookup.StorageVolume v : volumes) {
                        Folder f = FilesHelper.makeRoot(mAuthority, v);
                        subscriber.onNext(f);
                    }
                    subscriber.onCompleted();
                }
            }
        });
    }

    StorageLookup.StorageVolume getStorageVolume(String library) {
        StorageLookup.StorageVolume volume = null;
        try {
            volume = mStorageLookup.getStorageVolume(library);
            final File rootDir = new File(volume.path);
            if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                Timber.e("Can't access path %s", rootDir.getPath());
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "getStorageVolume(%s)", library);
        }
        return volume;
    }

    void browseFolders(String library, String identity, Subscriber<? super Model> subscriber, Bundle args) {
        final StorageLookup.StorageVolume volume = getStorageVolume(library);
        if (volume == null) {
            subscriber.onError(new IllegalArgumentException("Unknown volume " + library));
            return;
        }
        final File base = new File(volume.path);
        final File rootDir = StringUtils.isEmpty(identity) ? base : new File(base, identity);
        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
            subscriber.onError(new IllegalArgumentException("Can't access path " + rootDir.getPath()));
            return;
        }

        final File[] dirList = rootDir.listFiles();
        final List<File> files = new ArrayList<>(dirList.length);
        for (File f : dirList) {
            if (!f.canRead()) {
                continue;
            }
            if (StringUtils.startsWith(f.getName(), ".")) {
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
        final List<File> audioFiles = FilesHelper.filterAudioFiles(getContext(), files);
        final List<Track> tracks = FilesHelper.convertAudioFilesToTracks(getContext(),
                    mAuthority, volume, audioFiles);
        if (subscriber.isUnsubscribed()) {
            return;
        }
        for (Track track : tracks) {
            subscriber.onNext(track);
        }
        subscriber.onCompleted();
    }

    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        final StorageLookup.StorageVolume volume = getStorageVolume(library);
        if (volume == null) {
            subscriber.onError(new IllegalArgumentException("Unknown volume " + library));
            return;
        }
        final File rootDir = new File(volume.path);
        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
            subscriber.onError(new IllegalArgumentException("Can't access path " + rootDir.getPath()));
            return;
        }

        if (StringUtils.isNumeric(identity)) {
            Track track = FilesHelper.findTrack(getContext(), mAuthority, volume, identity);
            if (!subscriber.isUnsubscribed()) {
                if (track == null) {
                    subscriber.onError(new IllegalArgumentException("Unable to find track id=" + identity));
                } else {
                    subscriber.onNext(track);
                    subscriber.onCompleted();
                }
            }
        } else {
            final File f = new File(volume.path, identity);
            if(!subscriber.isUnsubscribed()) {
                if (!f.exists() || !f.isFile() || !f.canRead()) {
                    subscriber.onError(new IllegalArgumentException("Can't access file " + f.getPath()));
                } else {
                    subscriber.onNext(FilesHelper.makeTrackFromFile(mAuthority, volume, f));
                    subscriber.onCompleted();
                }
            }
        }
    }

    @Override
    protected Observable<List<Uri>> getDeleteObjsObservable(final List<Uri> uris, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<List<Uri>>() {
            @Override
            public void call(Subscriber<? super List<Uri>> subscriber) {
                List<Uri> tracks = new ArrayList<Uri>(uris.size());
                List<Uri> folders = new ArrayList<Uri>(uris.size());
                for (Uri uri : uris) {
                    switch (mUriMatcher.match(uri)) {
                        case FoldersUris.M_FOLDER:
                            folders.add(uri);
                            break;
                        case FoldersUris.M_TRACK_MS:
                        case FoldersUris.M_TRACK_PTH:
                            tracks.add(uri);
                            break;
                    }
                }
                List<Uri> deleted = new ArrayList<Uri>(uris.size());
                deleted.addAll(deleteTracks(tracks));
                deleted.addAll(deleteFolders(folders));
                subscriber.onNext(deleted);
                subscriber.onCompleted();
            }
        });
    }

    protected List<Uri> deleteTracks(List<Uri> tracks) {
        List<Uri> deleted = new ArrayList<>(tracks.size());
        for (Uri uri : tracks) {
            String id = uri.getLastPathSegment();
            if (StringUtils.isNumeric(id)) {
                int rem = FilesHelper.deleteTrack(getContext(), id);
                if (rem > 0) {
                    deleted.add(uri);
                }
            } else {
                String library = uri.getPathSegments().get(0);
                try {
                    File base = mStorageLookup.getStorageFile(library);
                    if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                        Timber.e("Can't access path %s", base.getPath());
                        throw new IllegalArgumentException("Can't access path " + base.getPath());
                    }
                    if (FilesHelper.deleteFile(base, id)) {
                        deleted.add(uri);
                    }
                } catch (IllegalArgumentException e) {
                    Timber.e(e, "deleteTracks %s/%s", library, id);
                }
            }
        }
        return deleted;
    }

    protected List<Uri> deleteFolders(List<Uri> folders) {
        List<Uri> deleted = new ArrayList<>(folders.size());
        for (Uri uri : folders) {
            String library = uri.getPathSegments().get(0);
            String id = uri.getLastPathSegment();
            try {
                File base = mStorageLookup.getStorageFile(library);
                if (!base.exists() || !base.isDirectory() || !base.canRead()) {
                    Timber.e("Can't access path %s", base.getPath());
                    throw new IllegalArgumentException("Can't access path " + base.getPath());
                }
                if (FilesHelper.deleteDirectory(getContext(), new File(base, id))) {
                    deleted.add(uri);
                }
            } catch (IllegalArgumentException e) {
                Timber.e(e, "deleteFolders %s/%s", library, id);
            }
        }
        return deleted;
    }

    @Override
    protected void createPlaylist(String name, PlaylistOperationListener<Uri> resultListener, Bundle extras) {
        Uri uri = PlaylistUtil.createPlaylist(getContext(), name);
        if (uri != null) {
            resultListener.onSuccess(uri);
            getContext().getContentResolver().notifyChange(FoldersUris.playlists(mAuthority), null);
        } else {
            resultListener.onError("Create failed");
        }
    }

    @Override
    protected void addToPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        int num = PlaylistUtil.addToPlaylist(getContext(), ids, plist);
        if (num > 0) {
            resultListener.onSuccess(PlaylistUtil.getPlaylist(getContext(), mAuthority, plist));
        } else {
            resultListener.onError("Insert failed");
        }
    }

    @Override
    protected void removeFromPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        int num = 0;
        for (String id : ids) {
            num += PlaylistUtil.removeFromPlaylist(getContext(), id, plist);
        }
        if (num > 0) {
            resultListener.onSuccess(PlaylistUtil.getPlaylist(getContext(), mAuthority, plist));
        } else {
            resultListener.onError("Remove failed");
        }
    }

    @Override
    protected void updatePlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Playlist> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        PlaylistUtil.clearPlaylist(getContext(), plist, false);
        int num = PlaylistUtil.addToPlaylist(getContext(), ids, plist);
        if (num > 0) {
            resultListener.onSuccess(PlaylistUtil.getPlaylist(getContext(), mAuthority, plist));
            getContext().getContentResolver().notifyChange(playlist, null);
        } else {
            resultListener.onError("Update failed");
        }
    }

    @Override
    protected void deletePlaylists(List<Uri> playlists, PlaylistOperationListener<Integer> resultListener, Bundle extras) {
        ContentResolver resolver = getContext().getContentResolver();
        int num = 0;
        for (Uri uri : playlists) {
            Uri msUri = Uris.PLAYLIST(uri.getLastPathSegment());
            num += resolver.delete(msUri, null, null);
        }
        if (num == playlists.size()) {
            resultListener.onSuccess(num);
            resolver.notifyChange(FoldersUris.playlists(mAuthority), null);
        } else {
            resultListener.onError("Delete failed");
        }
    }

    static String[] extractIds(List<Uri> tracks) {
        List<String> ids = new ArrayList<>(tracks.size());
        for (Uri uri : tracks) {
            String id = uri.getLastPathSegment();
            if (StringUtils.isNumeric(id)) {
                ids.add(id);
            }
        }
        return ids.toArray(new String[ids.size()]);
    }
}
