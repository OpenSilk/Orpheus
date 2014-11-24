/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.util.MarkedForRemoval;
import org.opensilk.music.ui2.loader.OrderPreservingCursor;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicProviderUtil;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;

import java.io.File;
import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {

    private static int sForegroundActivities = 0;

    private static ContentValues[] mContentValuesCache = null;

    /* This class is never initiated */
    private MusicUtils() {
    }

    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     *         albums, songs, genres, and playlists.
     */
    public static String makeLabel(final Context context, final int pluralInt,
            final int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    public static String makeTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs -= hours * 3600;
        mins = secs / 60;
        secs -= mins * 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    /**
     * Changes to the previous track.
     *
     * @NOTE The AIDL isn't used here in order to properly use the previous
     *       action. When the user is shuffling, because {@link
     *       MusicPlaybackService#openCurrentAndNext()} is used, the user won't
     *       be able to travel to the previously skipped track. To remedy this,
     *       {@link MusicPlaybackService#openCurrent()} is called in {@link
     *       MusicPlaybackService#prev()}. {@code #startService(Intent intent)}
     *       is called here to specifically invoke the onStartCommand used by
     *       {@link MusicPlaybackService}, which states if the current position
     *       less than 2000 ms, start the track over, otherwise move to the
     *       previously listened track.
     */
    public static void previous(final Context context) {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name The name of the playlist.
     * @return The ID for a playlist.
     */
    public static long getIdForPlaylist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] {
                    BaseColumns._ID
                }, PlaylistsColumns.NAME + "=?", new String[] {
                    name
                }, PlaylistsColumns.NAME);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    /**
     * Returns the Id for an artist.
     *
     * @param context The {@link Context} to use.
     * @param name The name of the artist.
     * @return The ID for an artist.
     */
    public static long getIdForArtist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] {
                    BaseColumns._ID
                }, ArtistColumns.ARTIST + "=?", new String[] {
                    name
                }, ArtistColumns.ARTIST);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    /**
     * Returns the ID for an album.
     *
     * @param context The {@link Context} to use.
     * @param albumName The name of the album.
     * @param artistName The name of the artist
     * @return The ID for an album.
     */
    public static long getIdForAlbum(final Context context, final String albumName, final String artistName) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] {
                    BaseColumns._ID
                }, AlbumColumns.ALBUM + "=? AND " + AlbumColumns.ARTIST + "=?", new String[] {
                    albumName, artistName
                }, AlbumColumns.ALBUM);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    /*  */
    public static void makeInsertItems(final long[] ids, final int offset, int len, final int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }

        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param name The name of the new playlist.
     * @return A new playlist ID.
     */
    public static long createPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[] {
                PlaylistsColumns.NAME
            };
            final String selection = PlaylistsColumns.NAME + " = '" + name + "'";
            Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    projection, selection, null, null);
            int count = 1;
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
            if (count <= 0) {
                final ContentValues values = new ContentValues(1);
                values.put(PlaylistsColumns.NAME, name);
                final Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values);
                return Long.parseLong(uri.getLastPathSegment());
            }
            return -1;
        }
        return -1;
    }

    /**
     * @param context The {@link Context} to use.
     * @param playlistId The playlist ID.
     */
    public static void clearPlaylist(final Context context, final int playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        context.getContentResolver().delete(uri, null, null);
        context.getContentResolver().notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
    }

    /**
     * @param context The {@link Context} to use.
     * @param ids The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(final Activity context, final long[] ids, final long playlistid) {
        final int size = ids.length;
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[] {
            "count(*)"
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        cursor.moveToFirst();
        final int base = cursor.getInt(0);
        cursor.close();
        cursor = null;
        int numinserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000) {
            makeInsertItems(ids, offSet, 1000, base);
            numinserted += resolver.bulkInsert(uri, mContentValuesCache);
        }
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
        Toast.makeText((Activity) context, message, Toast.LENGTH_LONG).show();
        context.getContentResolver().notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
    }

    /**
     * Removes a single track from a given playlist
     * @param context The {@link Context} to use.
     * @param id The id of the song to remove.
     * @param playlistId The id of the playlist being removed from.
     */
    public static void removeFromPlaylist(final Activity context, final long id,
            final long playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, Playlists.Members.AUDIO_ID + " = ? ", new String[] {
            Long.toString(id)
        });
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtracksfromplaylist, 1, 1);
        Toast.makeText((Activity)context, message, Toast.LENGTH_LONG).show();
        context.getContentResolver().notifyChange(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, null);
    }

    /**
     * @param context The {@link Context} to use
     * @param id The song ID.
     */
    public static MakeToast setRingtone(final Context context, final long id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (final UnsupportedOperationException ingored) {
            return new MakeToast(R.string.err_generic);
        }

        final String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.DATA, MediaColumns.TITLE
        };

        final String selection = BaseColumns._ID + "=" + id;
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                selection, null, null);
        try {
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                return new MakeToast(R.string.set_as_ringtone, cursor.getString(2));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return new MakeToast(R.string.err_generic);
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    @MarkedForRemoval @Deprecated
    public static void refresh() {

    }

    /**
     * Used to build and show a notification when Apollo is sent into the
     * background
     *
     * @param context The {@link Context} to use.
     */
    public static void notifyForegroundStateChanged(final Context context, boolean inForeground) {
        int old = sForegroundActivities;
        if (inForeground) {
            sForegroundActivities++;
        } else {
            sForegroundActivities--;
        }

        if (old == 0 || sForegroundActivities == 0) {
            final Intent intent = new Intent(context, MusicPlaybackService.class);
            intent.setAction(MusicPlaybackService.FOREGROUND_STATE_CHANGED);
            intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, sForegroundActivities != 0);
            context.startService(intent);
        }
    }

    /**
     * Perminately deletes item(s) from the user's device
     *
     * @param context The {@link Context} to use.
     * @param list The item(s) to delete.
     */
    public static int deleteTracks(final Context context, MusicServiceConnection musicService, final long[] list) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        final String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.DATA, AudioColumns.ALBUM_ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        final Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                null, null);
        if (c != null) {
            // Step 1: Remove selected tracks from the current playlist
            if (c.moveToFirst()) {
                do {
                    long id = MusicProviderUtil.getRecentId(context, c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID)));
                    if (id >= 0) {
                        // Remove from current playlist
                        musicService.removeTrack(id);
                        // Remove any items in the recents database
                        MusicProviderUtil.removeFromRecents(context, id);
                    }
                } while (c.moveToNext());
            }

            // Step 2: Remove selected tracks from the database
            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    selection.toString(), null);

            // Step 3: Remove files from card
            if (c.moveToFirst()) {
                do {
                    final String name = c.getString(1);
                    final File f = new File(name);
                    try { // File.delete can throw a security exception
                        if (!f.delete()) {
                            // I'm not sure if we'd ever get here (deletion would
                            // have to fail, but no exception thrown)
                            Log.e("MusicUtils", "Failed to delete file " + name);
                        }
                    } catch (final SecurityException ex) {
                    }
                } while (c.moveToNext());
            }
            c.close();
        } else {
            return 0;
        }
        // We deleted a number of tracks, which could affect any number of
        // things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        // Notify the lists to update
        refresh();

        return list.length;
    }
}
