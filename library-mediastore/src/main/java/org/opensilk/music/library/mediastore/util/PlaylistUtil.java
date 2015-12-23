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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.opensilk.music.library.mediastore.provider.FoldersUris;
import org.opensilk.music.library.mediastore.provider.StorageLookup;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 12/15/15.
 */
public class PlaylistUtil {

    public static List<Playlist> getPlaylists(final Context context, final String authority) {
        Cursor c = context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS,
                Projections.PLAYLIST, null, null, null);
        try {
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            List<Playlist> lst = new ArrayList<>(c.getCount());
            do {
                String id = c.getString(0);
                String name = c.getString(1);
                lst.add(Playlist.builder()
                        .setUri(FoldersUris.playlist(authority, id))
                        .setParentUri(FoldersUris.playlists(authority))
                        .setTracksUri(FoldersUris.playlistTracks(authority, id))
                        .setName(name)
                        .build());
            } while (c.moveToNext());
            return lst;
        } finally {
            if (c != null) c.close();
        }
    }

    public static Playlist getPlaylist(final Context context, final String authority, final String playlistId) {
        Cursor c = context.getContentResolver().query(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS,
                Projections.PLAYLIST, Selections.ID, new String[]{playlistId}, null);
        try {
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            String name = c.getString(1);
            return Playlist.builder()
                    .setUri(FoldersUris.playlist(authority, playlistId))
                    .setParentUri(FoldersUris.playlists(authority))
                    .setTracksUri(FoldersUris.playlistTracks(authority, playlistId))
                    .setName(name)
                    .build();
        } finally {
            if (c != null) c.close();
        }
    }

    public static List<Track> getPlaylistMembers(final Context context, String playlistId,
                                                 final String authority, List<StorageLookup.StorageVolume> volumes) {
        Cursor c = context.getContentResolver().query(Uris.PLAYLIST_MEMBERS(playlistId),
                Projections.PLAYLIST_SONGS, null, null, MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        try {
            if (c != null && c.moveToFirst()) {
                final List<Track> tracks = new ArrayList<>(c.getCount());
                do {
                    String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA));
                    File f = new File(path);
                    StorageLookup.StorageVolume volume = FilesHelper.guessStorageVolume(volumes, path);
                    if (volume == null) {
                        Timber.e("Unable to locate volume for %s", path);
                        continue;
                    }
                    Track.Builder tb = FilesHelper.makeTrackFromCursor(authority, volume, f, c);
                    tb.setTrackNumber(c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TRACK)));
                    tracks.add(tb.build());
                } while (c.moveToNext());
                return tracks;
            }
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    public static Uri createPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[] {
                    MediaStore.Audio.PlaylistsColumns.NAME
            };
            final String selection = MediaStore.Audio.PlaylistsColumns.NAME + "=?";
            Cursor cursor = resolver.query(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS,
                    projection, selection, new String[]{name}, null);
            int count = 1;
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
            if (count <= 0) {
                final ContentValues values = new ContentValues(1);
                values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
                final Uri uri = resolver.insert(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, values);
                if (uri != null) {
                    resolver.notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
                    return uri;
                }
            }
        }
        return null;
    }

    public static void clearPlaylist(final Context context, final String playlistId, boolean notify) {
        final Uri uri = Uris.PLAYLIST_MEMBERS(playlistId);
        ContentResolver resolver = context.getContentResolver();
        int num = resolver.delete(uri, null, null);
        if (notify) {
            resolver.notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
        }
    }

    static ContentValues[] makeInsertItems(final String[] ids, final int offset, int len, final int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }

        ContentValues[] contentValues = new ContentValues[len];
        for (int i = 0; i < len; i++) {
            contentValues[i] = new ContentValues();
            contentValues[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i);
            contentValues[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, Long.valueOf(ids[offset + i]));
        }
        return contentValues;
    }

    public static int addToPlaylist(final Context context, final String[] ids, final String playlistid) {
        final int size = ids.length;
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[] {
                "count(*)"
        };
        final Uri uri = Uris.PLAYLIST_MEMBERS(playlistid);
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        int base = 0;
        if (cursor != null && cursor.moveToFirst()) {
            base = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        int numinserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000) {
            ContentValues[] values = makeInsertItems(ids, offSet, 1000, base);
            numinserted += resolver.bulkInsert(uri, values);
        }
        resolver.notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
        return numinserted;
    }

    public static int removeFromPlaylist(final Context context, final String id, final String playlistId) {
        final Uri uri = Uris.PLAYLIST_MEMBERS(playlistId);
        final ContentResolver resolver = context.getContentResolver();
        int num = resolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?", new String[] {id});
        resolver.notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
        return num;
    }

}
