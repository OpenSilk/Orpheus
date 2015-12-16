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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.mediastore.R;
import org.opensilk.music.library.mediastore.util.CursorHelpers;
import org.opensilk.music.library.mediastore.util.FilesHelper;
import org.opensilk.music.library.mediastore.util.PlaylistUtil;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.StorageLookup;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.playlist.PlaylistOperationListener;
import org.opensilk.music.library.playlist.provider.PlaylistLibraryProvider;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateDataUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getIntOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getLongOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

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
    protected Observable<Model> getListObjsObservable(final Uri uri, final Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Model>() {
            @Override
            public void call(Subscriber<? super Model> subscriber) {
                switch (mUriMatcher.match(uri)) {
                    case FoldersUris.M_FOLDERS: {
                        browseFolders(uri.getPathSegments().get(0), null, subscriber, args);
                        break;
                    }
                    case FoldersUris.M_FOLDER: {
                        browseFolders(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                        break;
                    }
                    case FoldersUris.M_PLAYLISTS: {
                        List<Playlist> playlists = PlaylistUtil.getPlaylists(getContext(), mAuthority);
                        if (playlists == null) {
                            subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                        } else if (!subscriber.isUnsubscribed()) {
                            for (Playlist p : playlists) {
                                subscriber.onNext(p);
                            }
                            subscriber.onCompleted();
                        }
                        break;
                    }
                    case FoldersUris.M_PLAYLIST: {
                        listPlaylist(uri.getLastPathSegment(), subscriber, args);
                        break;
                    }
                    default:
                        Timber.w("Unmatched uri %s", uri);
                        subscriber.onError(new LibraryException(LibraryException.Kind.ILLEGAL_URI,
                                new IllegalArgumentException("Invalid uri " + uri)));
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

        }
        return volume;
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
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(FilesHelper.makeRoot(mAuthority, volume));
                            subscriber.onCompleted();
                        }
                        return;
                    }
                    case FoldersUris.M_FOLDER: {
                        final String library = uri.getPathSegments().get(0);
                        final StorageLookup.StorageVolume volume = getStorageVolume(library);
                        if (library == null) {
                            subscriber.onError(new IllegalArgumentException("Can't access volume " + library));
                            return;
                        }
                        final String identity = uri.getLastPathSegment();
                        final File rootDir = new File(volume.path, identity);
                        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                            Timber.e("Can't access path %s", rootDir.getPath());
                            subscriber.onError(new IllegalArgumentException("Can't access path " + rootDir.getPath()));
                            return;
                        }
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(FilesHelper.makeFolder(mAuthority, volume, rootDir));
                            subscriber.onCompleted();
                        }
                        return;
                    }
                    case FoldersUris.M_TRACK_MS:
                    case FoldersUris.M_TRACK_PTH: {
                        getTrack(uri.getPathSegments().get(0), uri.getLastPathSegment(), subscriber, args);
                        break;
                    }
                    case FoldersUris.M_PLAYLIST: {
                        Playlist playlist = PlaylistUtil.getPlaylist(getContext(), mAuthority, uri.getLastPathSegment());
                        if (!subscriber.isUnsubscribed()) {
                            if (playlist != null) {
                                subscriber.onNext(playlist);
                                subscriber.onCompleted();
                            } else {
                                subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                            }
                        }
                        break;
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

    void browseFolders(String library, String identity, Subscriber<? super Model> subscriber, Bundle args) {
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
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
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
        tracks = FilesHelper.convertAudioFilesToTracks(getContext(),
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
        final StorageLookup.StorageVolume volume;
        try {
            volume = mStorageLookup.getStorageVolume(library);
            final File rootDir = new File(volume.path);
            if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
                Timber.e("Can't access path %s", rootDir.getPath());
                throw new IllegalArgumentException("Can't access path " + rootDir.getPath());
            }
        } catch (IllegalArgumentException e) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
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

    void listPlaylist(String id, Subscriber<? super Model> subscriber, Bundle args) {
        Cursor c = getContext().getContentResolver().query(Uris.PLAYLIST_MEMBERS(id),
                Projections.PLAYLIST_SONGS, null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        try {
            if (c == null || !c.moveToFirst()) {
                subscriber.onError(new NullPointerException("Unable to obtain cursor"));
                return;
            }
            List<StorageLookup.StorageVolume> volumes = mStorageLookup.getStorageVolumes();
            do {
                if (subscriber.isUnsubscribed()) break;
                Track.Builder tb = Track.builder()
                        .setUri(FoldersUris.track(mAuthority, getTrackVolume(volumes,
                                c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA))),
                                c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))))
                        .setParentUri(FoldersUris.playlist(mAuthority, id))
                        .setSortName(getStringOrNull(c, MediaStore.Audio.AudioColumns.DISPLAY_NAME))
                        .setName(getStringOrNull(c, MediaStore.Audio.AudioColumns.TITLE))
                        .setArtistName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ARTIST))
                        .setAlbumName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM))
                        .setTrackNumber(getIntOrZero(c, MediaStore.Audio.Media.TRACK))
                        .setArtworkUri(generateArtworkUri(
                                c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))))
//                        .setFlags(getFlags(f))
                        .addRes(Track.Res.builder()
                                        .setUri(generateDataUri(c.getString(c.getColumnIndexOrThrow(BaseColumns._ID))))
                                        .setMimeType(getStringOrNull(c, MediaStore.Audio.AudioColumns.MIME_TYPE))
                                        .setDuration(getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION))
                                        .setLastMod(getLongOrZero(c, MediaStore.Audio.AudioColumns.DATE_MODIFIED))
                                        .setSize(getLongOrZero(c, MediaStore.Audio.AudioColumns.SIZE))
                                        .build()
                        )
                        ;
                subscriber.onNext(tb.build());
            } while (c.moveToNext());
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        } finally {
            if (c != null) c.close();
        }
    }

    static int getTrackVolume(List<StorageLookup.StorageVolume> volumes, String path) {
        if (volumes != null && volumes.size() != 0) {
            for (StorageLookup.StorageVolume v : volumes) {
                if (StringUtils.startsWith(path, v.path)) {
                    return v.id;
                }
            }
        }
        return 0;
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
        } else {
            resultListener.onError("Create failed");
        }
    }

    @Override
    protected void addToPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Integer> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        int num = PlaylistUtil.addToPlaylist(getContext(), ids, plist);
        if (num > 0) {
            resultListener.onSuccess(num);
        } else {
            resultListener.onError("Insert failed");
        }
    }

    @Override
    protected void removeFromPlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Integer> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        int num = 0;
        for (String id : ids) {
            num += PlaylistUtil.removeFromPlaylist(getContext(), id, plist);
        }
        if (num > 0) {
            resultListener.onSuccess(num);
        } else {
            resultListener.onError("Remove failed");
        }
    }

    @Override
    protected void updatePlaylist(Uri playlist, List<Uri> tracks, PlaylistOperationListener<Integer> resultListener, Bundle extras) {
        String[] ids = extractIds(tracks);
        String plist = playlist.getLastPathSegment();
        PlaylistUtil.clearPlaylist(getContext(), plist);
        int num = PlaylistUtil.addToPlaylist(getContext(), ids, plist);
        if (num > 0) {
            resultListener.onSuccess(num);
        } else {
            resultListener.onError("Update failed");
        }
    }

    @Override
    protected void deletePlaylists(List<Uri> playlists, PlaylistOperationListener<Integer> resultListener, Bundle extras) {
        ContentResolver resolver = getContext().getContentResolver();
        int num = 0;
        for (Uri uri : playlists) {
            num += resolver.delete(uri, null, null);
        }
        if (num == playlists.size()) {
            resultListener.onSuccess(num);
        } else {
            resultListener.onError("Delete failed");
        }
    }

    static String[] extractIds(List<Uri> tracks) {
        String[] ids = new String[tracks.size()];
        for (int ii=0; ii<tracks.size(); ii++) {
            ids[ii] = tracks.get(ii).getLastPathSegment();
        }
        return ids;
    }
}
