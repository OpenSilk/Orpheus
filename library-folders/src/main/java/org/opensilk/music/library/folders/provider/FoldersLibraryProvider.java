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

package org.opensilk.music.library.folders.provider;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.folders.FoldersComponent;
import org.opensilk.music.library.folders.ModelUtil;
import org.opensilk.music.library.folders.R;
import org.opensilk.music.library.folders.StorageLookup;
import org.opensilk.music.library.folders.ui.StoragePickerActivity;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import mortar.MortarScope;
import rx.Subscriber;
import timber.log.Timber;

import static org.opensilk.music.library.LibraryCapability.*;
import static org.opensilk.music.library.folders.ModelUtil.*;
import static org.opensilk.music.library.provider.LibraryMethods.Extras.*;
import static org.opensilk.music.library.provider.LibraryUris.Q.*;

/**
 * Created by drew on 4/28/15.
 */
public class FoldersLibraryProvider extends LibraryProvider {
    public static final String TAG = FoldersLibraryProvider.class.getName();

    @Inject StorageLookup mStorageLookup;

    @Override
    public boolean onCreate() {
        MortarScope rootScope = MortarScope.getScope(getContext().getApplicationContext());
        AppContextComponent acc = DaggerService.getDaggerComponent(rootScope);
        FoldersComponent.FACTORY.call(acc).inject(this);
        return super.onCreate();
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .addAbility(FOLDERSTRACKS)
                //.addAbility(TRACKS)
                .setAuthority(mAuthority)
                .setPickerComponent(new ComponentName(getContext(), StoragePickerActivity.class),
                        getContext().getString(R.string.folders_picker_title))
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return getContext().getPackageName() + ".provider.foldersLibrary";
    }

    @Override
    protected void browseFolders(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        doListing(library, identity, subscriber, args);
    }

    @Override
    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        super.queryTracks(library, subscriber, args);
    }

    @Override
    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        super.getTrack(library, identity, subscriber, args);
    }

    void doListing(String library, String identity, Subscriber<? super Bundleable> subscriber, Bundle args) {
        final File base = mStorageLookup.getStorageFile(library);
        final File rootDir = TextUtils.isEmpty(identity) ? base : new File(base, identity);
        if (!rootDir.exists() || !rootDir.isDirectory() || !rootDir.canRead()) {
            Timber.w("Can't access storage path %s", base.getPath());
            subscriber.onCompleted();
            return;
        }

        final String q = args.<Uri>getParcelable(URI).getQueryParameter(Q);
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
                subscriber.onNext(makeFolder(base, f));
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
        List<File> audioFiles = filterAudioFiles(getContext(), files);
        List<Track> tracks = convertAudioFilesToTracks(getContext(), base, audioFiles);
        if (subscriber.isUnsubscribed()) {
            return;
        }
        for (Track track : tracks) {
            subscriber.onNext(track);
        }
        subscriber.onCompleted();
    }

    @NonNull
    public static List<File> filterAudioFiles(Context context, List<File> files) {
        if (files.size() == 0) {
            return Collections.emptyList();
        }
        //Map for cursor
        final HashMap<String, File> pathMap = new HashMap<>();
        //The returned list
        final List<File> audioFiles = new ArrayList<>();

        //Build the selection
        final int size = files.size();
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Files.FileColumns.DATA + " IN (");
        for (int i = 0; i < size; i++) {
            final File f = files.get(i);
            final String path = f.getAbsolutePath();
            pathMap.put(path, f); //Add file to map while where iterating
            selection.append("'").append(path).append("'");
            if (i < size - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    MEDIA_TYPE_PROJECTION,
                    selection.toString(),
                    null,
                    null);
            if (c != null && c.moveToFirst()) {
                do {
                    final int mediaType = c.getInt(0);
                    final String path = c.getString(1);
                    final File f = pathMap.remove(path);
                    if (f != null && mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) {
                        audioFiles.add(f);
                    } //else throw away
                } while (c.moveToNext());
            }
            //either the query failed or the cursor didn't contain all the files we asked for.
            if (!pathMap.isEmpty()) {
                Timber.w("%d files weren't found in mediastore. Best guessing mime type", pathMap.size());
                for (File f : pathMap.values()) {
                    final String mime = guessMimeType(f);
                    if (StringUtils.contains(mime, "audio") || "application/ogg".equals(mime)) {
                        audioFiles.add(f);
                    }
                }
            }
        } catch (Exception e) {
            if (ModelUtil.DUMPSTACKS) Timber.e(e, "filterAudioFiles");
        } finally {
            closeQuietly(c);
        }
        return audioFiles;
    }

    public static List<Track> convertAudioFilesToTracks(Context context, File base, List<File> audioFiles) {
        if (audioFiles.size() == 0) {
            return Collections.emptyList();
        }

        final List<Track> trackList = new ArrayList<>(audioFiles.size());

        Cursor c = null;
        Cursor c2 = null;
        try {
            final HashMap<String, File> pathMap = new HashMap<>();

            //Build the selection
            final int size = audioFiles.size();
            final StringBuilder selection = new StringBuilder();
            selection.append(MediaStore.Audio.AudioColumns.DATA + " IN (");
            for (int i = 0; i < size; i++) {
                final File f = audioFiles.get(i);
                final String path = f.getAbsolutePath();
                pathMap.put(path, f); //Add file to map while where iterating
                selection.append("'").append(path).append("'");
                if (i < size - 1) {
                    selection.append(",");
                }
            }
            selection.append(")");

            //make query
            c = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    SONG_PROJECTION,
                    selection.toString(),
                    null,
                    null);
            if (c != null && c.moveToFirst()) {
                //track albums for second query
                final HashMap<Long, List<Track.Builder>> albumsMap = new HashMap<>();
                do {
                    final File f = pathMap.remove(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)));
                    if (f != null) {
                        Track.Builder tb = Track.builder()
                                .setIdentity(toRelativePath(base, f))
                                .setName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)))
                                .setArtistName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)))
                                .setAlbumName(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)))
                                .setAlbumIdentity(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)))
                                .setDuration(c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)) / 1000)
                                .setMimeType(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)))
                                .setDataUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))))
                                .setArtworkUri(ContentUris.withAppendedId(BASE_ARTWORK_URI,
                                        c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
                        long albumId = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                        List<Track.Builder> tsForAlbum = albumsMap.get(albumId);
                        if (tsForAlbum == null) {
                            tsForAlbum = new ArrayList<>();
                            albumsMap.put(albumId, tsForAlbum);
                        }
                        tsForAlbum.add(tb);
                    }
                } while (c.moveToNext());

                //Get the albumartists
                final Set<Long> albumIds = albumsMap.keySet();
                final int size2 = albumIds.size();
                final StringBuilder selection2 = new StringBuilder();
                selection2.append(BaseColumns._ID + " IN (");
                int i = 0;
                for (long id : albumIds) {
                    selection2.append(id);
                    if (++i < size2) {
                        selection2.append(",");
                    }
                }
                selection2.append(")");
                c2 = context.getContentResolver().query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        SONG_ALBUM_PROJECTION,
                        selection2.toString(),
                        null,
                        null);
                if (c2 != null && c2.moveToFirst()) {
                    do {
                        final long id = c2.getLong(0);
                        final String artist = c2.getString(1);
                        List<Track.Builder> albumTracks = albumsMap.remove(id);
                        if (albumTracks != null) {
                            for (Track.Builder tb : albumTracks) {
                                tb.setAlbumArtistName(artist);
                                trackList.add(tb.build());
                            }
                        }
                    } while (c2.moveToNext());
                }
                if (!albumsMap.isEmpty()) {
                    Timber.w("%d albums didn't make the cursor", albumsMap.size());
                    for (List<Track.Builder> tbs : albumsMap.values()) {
                        for (Track.Builder tb : tbs) {
                            trackList.add(tb.build());
                        }
                    }
                }
            }
            if (!pathMap.isEmpty()) {
                Timber.w("%d audioFiles didn't make the cursor", pathMap.size());
                for (File f : pathMap.values()) {
                    Track t = Track.builder()
                            .setIdentity(toRelativePath(base, f))
                            .setName(f.getName())
                            .setMimeType(guessMimeType(f))
                            .setDataUri(Uri.fromFile(f))
                            .build();
                    trackList.add(t);
                }
            }
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "convertAudioFilesToTracks");
        } finally {
            closeQuietly(c);
            closeQuietly(c2);
        }
        return trackList;
    }
}
