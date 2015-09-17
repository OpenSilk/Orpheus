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

package org.opensilk.music.library.mediastore.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.library.mediastore.BuildConfig;
import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.library.mediastore.util.StorageLookup.StorageVolume;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateDataUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getIntOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getLongOrZero;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrNull;

/**
 * Created by drew on 4/28/15.
 */
public class FilesHelper {
    public static final boolean DUMPSTACKS = BuildConfig.DEBUG;

    private static final DateFormat sDateFormat;

    static {
        sDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    public static String getFileExtension(String name) {
        String ext;
        int lastDot = name.lastIndexOf('.');
        int secondLastDot = name.lastIndexOf('.', lastDot - 1);
        if (secondLastDot > 0 ) { // Double extension
            ext = name.substring(secondLastDot + 1);
            if (!ext.startsWith("tar")) {
                ext = name.substring(lastDot + 1);
            }
        } else if (lastDot > 0) { // Single extension
            ext = name.substring(lastDot + 1);
        } else { // No extension
            ext = "";
        }
        return ext;
    }

    public static String getFileExtension(File f) {
        return getFileExtension(f.getName());
    }

    public static String guessMimeType(File f) {
        return guessMimeType(getFileExtension(f));
    }

    public static String guessMimeType(String ext) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mimeType == null) {
            mimeType = "*/*";
        }
        return mimeType;
    }

    @NonNull
    public static String formatDate(long ms) {
        return sDateFormat.format(new Date(ms));
    }

    /*
     * Android external storage paths have changed numerous times since
     * ive been watching so we use relative paths for ids so we don't get screwed
     * up when it changes again
     */
    @NonNull
    public static String toRelativePath(File base, File f) {
        String p = StringUtils.replace(f.getAbsolutePath(), base.getAbsolutePath(), "", 1);
        return !p.startsWith("/") ? p : p.substring(1);
    }

    public static String toRelativePath(String base, File f) {
        return toRelativePath(new File(base), f);
    }

    static @NonNull Uri findParentUri(String authority, StorageVolume volume, File f) {
        final File parent = f.getParentFile();
        final Uri parentUri;
        if (StringUtils.equals(volume.path, f.getAbsolutePath())
                || parent == null
                || StringUtils.equals(volume.path, parent.getAbsolutePath())) {
            parentUri = FoldersUris.folders(authority, String.valueOf(volume.id));
        } else {
            parentUri = FoldersUris.folder(authority, String.valueOf(volume.id), toRelativePath(volume.path, parent));
        }
        return parentUri;
    }

    @NonNull
    public static Folder makeFolder(String authority, StorageVolume volume, File dir) {
        final String[] children = dir.list();
        return Folder.builder()
                .setUri(FoldersUris.folder(authority, String.valueOf(volume.id), toRelativePath(volume.path, dir)))
                .setParentUri(findParentUri(authority, volume, dir))
                .setName(dir.getName())
                .setChildCount(children != null ? children.length : 0)
                .setDateModified(formatDate(dir.lastModified()))
                .build();
    }

    public static Track makeTrackFromFile(String authority, StorageVolume volume, File f) {
        return Track.builder()
                .setUri(FoldersUris.track(authority, String.valueOf(volume.id), toRelativePath(volume.path, f)))
                .setParentUri(findParentUri(authority, volume, f))
                .setName(f.getName())
                .addRes(Track.Res.builder().setUri(Uri.fromFile(f)).setMimeType(guessMimeType(f)).build())
                .build();
    }

    public static int deleteFiles(File base, List<String> relPaths) {
        int numdeleted = 0;
        for (String name : relPaths) {
            try {
                final File f = new File(base, name);
                if (f.exists() && f.isFile() && f.canWrite()) {
                    if (!f.delete()) {
                        Timber.w("Unable to delete file %s", f.getName());
                    } else {
                        numdeleted++;
                    }
                }
            } catch (SecurityException e) {
            }
        }
        return numdeleted;
    }

    public static boolean deleteDirectory(Context context, File dir) {
        boolean success = false;
        if (dir.exists() && dir.isDirectory() && dir.canWrite()) {
            Cursor c = null;
            try {
                 c = context.getContentResolver().query(
                        Uris.EXTERNAL_MEDIASTORE_FILES,
                        Projections.ID_DATA,
                        MediaStore.Files.FileColumns.DATA + " GLOB ?",
                        new String[]{dir.getAbsolutePath()+"*"},
                        null
                );
                if (c != null && c.moveToFirst()) {
                    StringBuilder selection = new StringBuilder(100);
                    selection.append(BaseColumns._ID + " IN (");
                    while (true) {
                        selection.append(c.getLong(0));
                        Timber.d("Adding %s", c.getString(1));
                        if (!c.moveToNext()) {
                            break;
                        }
                        selection.append(",");
                    }
                    selection.append(")");
                    Timber.d("Removing %s", selection.toString());
                    //remove from mediastore
                    //TODO just delete on the glob
                    context.getContentResolver().delete(
                            Uris.EXTERNAL_MEDIASTORE_FILES,
                            selection.toString(), null);
                }
            } finally {
                closeQuietly(c);
            }
            try {
                Timber.d("Deleting dir %s", dir.getPath());
                FileUtils.deleteDirectory(dir);
                success = true;
            } catch (IOException e) {
                success = false;
            }
            //notify on everything
            context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        }
        return success;
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
        final StringBuilder selection = new StringBuilder(size * 2 + 20);
        final String[] selectionArgs = new String[size];
        selection.append(MediaStore.Files.FileColumns.DATA + " IN (");
        for (int i = 0; i < size; i++) {
            final File f = files.get(i);
            final String path = f.getAbsolutePath();
            selection.append("?");
            if (i < size - 1) {
                selection.append(",");
            }
            selectionArgs[i] = path;
            pathMap.put(path, f); //Add file to map while where iterating
        }
        selection.append(")");
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    Uris.EXTERNAL_MEDIASTORE_FILES,
                    Projections.MEDIA_TYPE_PROJECTION,
                    selection.toString(),
                    selectionArgs,
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
                //TODO add files to mediastore
                for (File f : pathMap.values()) {
                    final String mime = guessMimeType(f);
                    if (StringUtils.contains(mime, "audio") || "application/ogg".equals(mime)) {
                        audioFiles.add(f);
                    }
                }
            }
        } catch (Exception e) {
            if (FilesHelper.DUMPSTACKS) Timber.e(e, "filterAudioFiles");
        } finally {
            closeQuietly(c);
        }
        return audioFiles;
    }

    @NonNull
    public static List<Track> convertAudioFilesToTracks(Context context, String authority, StorageVolume volume, List<File> audioFiles) {
        if (audioFiles.size() == 0) {
            return Collections.emptyList();
        }

        final List<Track> trackList = new ArrayList<>(audioFiles.size());

        Cursor c = null;
        try {
            final HashMap<String, File> pathMap = new HashMap<>();

            //Build the selection
            final int size = audioFiles.size();
            final StringBuilder selection = new StringBuilder(size * 2 + 20);
            final String[] selectionArgs = new String[size];
            selection.append(MediaStore.Audio.AudioColumns.DATA + " IN (");
            for (int i = 0; i < size; i++) {
                final File f = audioFiles.get(i);
                final String path = f.getAbsolutePath();
                selection.append("?");
                if (i < size - 1) {
                    selection.append(",");
                }
                selectionArgs[i] = path;
                pathMap.put(path, f); //Add file to map while where iterating
            }
            selection.append(")");

            //make query
            c = context.getContentResolver().query(
                    Uris.EXTERNAL_MEDIASTORE_MEDIA,
                    Projections.SONG_FILE,
                    selection.toString(),
                    selectionArgs,
                    null);
            if (c != null && c.moveToFirst()) {
                do {
                    final File f = pathMap.remove(c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)));
                    if (f != null) {
                        try {
                            Track.Builder tb = Track.builder()
                                    .setUri(FoldersUris.track(authority, volume.id, c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))))
                                    .setParentUri(findParentUri(authority, volume, f))
                                    .setName(getStringOrNull(c, MediaStore.Audio.AudioColumns.DISPLAY_NAME))
                                    .setArtistName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ARTIST))
                                    .setAlbumName(getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM))
                                    .setAlbumArtistName(getStringOrNull(c, "album_artist"))
//                                    .setAlbumIdentity(getStringOrNull(c, MediaStore.Audio.AudioColumns.ALBUM_ID))
                                    .setDuration((int) (getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION) / 1000))
                                    .addRes(Track.Res.builder()
                                            .setUri(generateDataUri(c.getString(c.getColumnIndexOrThrow(BaseColumns._ID))))
                                            .setMimeType(getStringOrNull(c, MediaStore.Audio.AudioColumns.MIME_TYPE))
                                            .setDuration(getLongOrZero(c, MediaStore.Audio.AudioColumns.DURATION))
                                            .build()
                                    )
                                    .setArtworkUri(generateArtworkUri(
                                            c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))))
                                    .setTrackNumber(getIntOrZero(c, MediaStore.Audio.AudioColumns.TRACK))
                                    ;
                            trackList.add(tb.build());
                        } catch (IllegalArgumentException ignored) {}
                    }
                } while (c.moveToNext());
            }
            if (!pathMap.isEmpty()) {
                Timber.w("%d audioFiles didn't make the cursor", pathMap.size());
                for (File f : pathMap.values()) {
                    trackList.add(makeTrackFromFile(authority, volume, f));
                }
            }
        } catch (Exception e) {
            if (DUMPSTACKS) Timber.e(e, "convertAudioFilesToTracks");
        } finally {
            closeQuietly(c);
        }
        return trackList;
    }

    public static void closeQuietly(Cursor c) {
        try {
            if (c != null) c.close();
        } catch (Exception ignored) { }
    }
}
