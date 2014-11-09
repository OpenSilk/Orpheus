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
import org.opensilk.music.R;
import org.opensilk.music.util.MarkedForRemoval;
import org.opensilk.music.util.OrderPreservingCursor;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.provider.RecentStore;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

import java.io.File;
import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {

    public static IApolloService sService = null;

    private static int sForegroundActivities = 0;

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap;

    private static final long[] sEmptyList;
    private static final Song[] sEmptySongList;
    private static final LocalSong[] sEmptyLocalSongList;

    private static ContentValues[] mContentValuesCache = null;

    static {
        mConnectionMap = new WeakHashMap<Context, ServiceBinder>();
        sEmptyList = new long[0];
        sEmptySongList = new Song[0];
        sEmptyLocalSongList = new LocalSong[0];
    }

    /* This class is never initiated */
    public MusicUtils() {
    }

    /**
     * @param context The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static final ServiceToken bindToService(final Context context,
            final ServiceConnection callback) {
        final ContextWrapper contextWrapper;
        if (context instanceof Activity) {
            Activity realActivity = ((Activity)context).getParent();
            if (realActivity == null) {
                realActivity = (Activity) context;
            }
            contextWrapper = new ContextWrapper(realActivity);
        } else {
            contextWrapper = new ContextWrapper(context);
        }
        contextWrapper.startService(new Intent(contextWrapper, MusicPlaybackService.class));
        final ServiceBinder binder = new ServiceBinder(callback);
        if (contextWrapper.bindService(
                new Intent().setClass(contextWrapper, MusicPlaybackService.class), binder, 0)) {
            mConnectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper mContextWrapper = token.mWrappedContext;
        final ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
        if (mBinder == null) {
            return;
        }
        mContextWrapper.unbindService(mBinder);
        if (mConnectionMap.isEmpty()) {
            sService = null;
        }
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;

        /**
         * Constructor of <code>ServiceBinder</code>
         *
         * @param context The {@link ServiceConnection} to use
         */
        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            sService = IApolloService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }

    public static final class ServiceToken {
        public ContextWrapper mWrappedContext;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The {@link ContextWrapper} to use
         */
        public ServiceToken(final ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    /**
     * Used by activity to decide whether it can show error dialogs
     * @return
     */
    public static boolean isForeground() {
        return sForegroundActivities > 0;
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
    public static final String makeLabel(final Context context, final int pluralInt,
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
    public static final String makeTimeString(final Context context, long secs) {
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
     * Changes to the next track
     */
    public static void next() {
        try {
            if (sService != null) {
                sService.next();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Changes to the previous track.
     *
     * @NOTE The AIDL isn't used here in order to properly use the previous
     *       action. When the user is shuffling, because {@link
     *       MusicPlaybackService.#openCurrentAndNext()} is used, the user won't
     *       be able to travel to the previously skipped track. To remedy this,
     *       {@link MusicPlaybackService.#openCurrent()} is called in {@link
     *       MusicPlaybackService.#prev()}. {@code #startService(Intent intent)}
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
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            if (sService != null) {
                if (sService.isPlaying()) {
                    sService.pause();
                } else {
                    sService.play();
                }
            }
        } catch (final Exception ignored) {
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            if (sService != null) {
                switch (sService.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        sService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;
                    case MusicPlaybackService.REPEAT_ALL:
                        sService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        if (sService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                            sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        }
                        break;
                    default:
                        sService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        try {
            if (sService != null) {
                switch (sService.getShuffleMode()) {
                    case MusicPlaybackService.SHUFFLE_NONE:
                        sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
                        if (sService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                            sService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        }
                        break;
                    case MusicPlaybackService.SHUFFLE_NORMAL:
                        sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    case MusicPlaybackService.SHUFFLE_AUTO:
                        sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    default:
                        break;
                }
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static final boolean isPlaying() {
        if (sService != null) {
            try {
                return sService.isPlaying();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    /**
     * @return The current shuffle mode.
     */
    public static final int getShuffleMode() {
        if (sService != null) {
            try {
                return sService.getShuffleMode();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The current repeat mode.
     */
    public static final int getRepeatMode() {
        if (sService != null) {
            try {
                return sService.getRepeatMode();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The current track name.
     */
    public static final String getTrackName() {
        if (sService != null) {
            try {
                return sService.getTrackName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current artist name.
     */
    public static final String getArtistName() {
        if (sService != null) {
            try {
                return sService.getArtistName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current album name.
     */
    public static final String getAlbumName() {
        if (sService != null) {
            try {
                return sService.getAlbumName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return Name of artist associated with album of currently playing song
     */
    public static String getAlbumArtistName() {
        if (sService != null) {
            try {
                return sService.getAlbumArtistName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current album Id.
     */
    public static final long getCurrentAlbumId() {
        if (sService != null) {
            try {
                return sService.getAlbumId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The current song Id.
     */
    public static final long getCurrentAudioId() {
        if (sService != null) {
            try {
                return sService.getAudioId();
            } catch (final RemoteException ignored) {
            }
        }
        return -1;
    }

    /**
     * @return The audio session Id.
     */
    public static final int getAudioSessionId() {
        if (sService != null) {
            try {
                return sService.getAudioSessionId();
            } catch (final RemoteException ignored) {
            }
        }
        return AudioEffect.ERROR_BAD_VALUE;
    }

    /**
     * @param context
     * @return the currently playing album
     */
    public static Album getCurrentAlbum(final Context context) {
        long albumId = getCurrentAlbumId();
        return makeLocalAlbum(context, albumId);
    }

    public static ArtInfo getCurrentArtInfo() {
        if (sService != null) {
            try {
                return sService.getCurrentArtInfo();
            } catch (RemoteException ignored) {}
        }
        return null;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the album.
     * @return new album.
     */
    public static LocalAlbum makeLocalAlbum(final Context context, long albumId) {
        LocalAlbum album = null;
        Cursor c = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_ALBUM,
                BaseColumns._ID +"=?",
                new String[]{String.valueOf(albumId)},
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        if (c != null && c.moveToFirst()) {
            album = CursorHelpers.makeLocalAlbumFromCursor(c);
        }
        if (c != null) {
            c.close();
        }
        return album;
    }

    /**
     * Creates artist object from their name;
     * @param context
     * @param artistName
     * @return
     */
    public static LocalArtist makeArtist(final Context context, final String artistName) {
        LocalArtist artist = null;
        final String selection = MediaStore.Audio.ArtistColumns.ARTIST  + "=?";
        final String[] selectionArgs = new String[] { artistName };
        final Cursor c = context.getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_ARTIST,
                selection,
                selectionArgs,
                null);
        if (c != null && c.moveToFirst()) {
            artist = CursorHelpers.makeLocalArtistFromCursor(c);
        }
        if (c != null) {
            c.close();
        }
        return artist;
    }

    /**
     * @return true if currently casting
     */
    public static boolean isRemotePlayback() {
        if (sService != null) {
            try {
                return sService.isRemotePlayback();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    /**
     * @return The queue.
     */
    public static final long[] getQueue() {
        try {
            if (sService != null) {
                return sService.getQueue();
            } else {
            }
        } catch (final RemoteException ignored) {
        }
        return sEmptyList;
    }

    /**
     * @param id The ID of the track to remove.
     * @return removes track from a playlist or the queue.
     */
    @MarkedForRemoval
    public static final int removeTrackOLD(final long id) {
        try {
            if (sService != null) {
                return sService.removeTrack(id);
            }
        } catch (final RemoteException ingored) {
        }
        return 0;
    }

    public static int removeQueueItem(long id) {
        try {
            if (sService != null) {
                return sService.removeTrack(id);
            }
        } catch (final RemoteException ingored) {
        }
        return 0;
    }

    public static int removeSong(Context context, Song song) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        long id = MusicProviderUtil.getIdForSong(context, song);
        if (id >= 0) {
            return removeQueueItem(id);
        }
        return 0;
    }

    /**
     * @return The position of the current track in the queue.
     */
    public static final int getQueuePosition() {
        try {
            if (sService != null) {
                return sService.getQueuePosition();
            }
        } catch (final RemoteException ignored) {
        }
        return 0;
    }

    /**
     * @param cursor The {@link Cursor} used to perform our query.
     * @return The song list for a MIME type.
     */
    public static final long[] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final long[] list = new long[len];
        cursor.moveToFirst();
        int columnIndex = -1;
        try {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (final IllegalArgumentException notaplaylist) {
            columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(columnIndex);
            cursor.moveToNext();
        }
        cursor.close();
        cursor = null;
        return list;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the artist.
     * @return The song list for an artist.
     */
    @Deprecated
    @MarkedForRemoval
    public static final long[] getSongListForArtist(final Context context, final long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        final String selection = AudioColumns.ARTIST_ID + "=" + id + " AND "
                + AudioColumns.IS_MUSIC + "=1";
                Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK);
        if (cursor != null) {
            final long[] mList = getSongListForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the artist.
     * @return The song list for an artist.
     */
    @MarkedForRemoval
    public static LocalSong[] getLocalSongListForArtist(final Context context, final long id) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LOCAL_ARTIST_SONGS,
                SelectionArgs.LOCAL_ARTIST_SONGS(id),
                AudioColumns.ALBUM_KEY + ", " + AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            LocalSong[] songs = new LocalSong[cursor.getCount()];
            if (cursor.moveToFirst()) {
                int ii= 0;
                do {
                    songs[ii++] = CursorHelpers.makeLocalSongFromCursor(context, cursor);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return songs;
        }
        return sEmptyLocalSongList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The ID of the genre.
     * @return The song list for an genre.
     */
    public static final long[] getSongListForGenre(final Context context, final long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + "!=''");
        final Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", Long.valueOf(id));
        Cursor cursor = context.getContentResolver().query(uri, projection, selection.toString(),
                null, null);
        if (cursor != null) {
            final long[] mList = getSongListForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return sEmptyList;
    }

    @MarkedForRemoval
    public static LocalSong[] getLocalSongListForGenre(final Context context, final long id) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Genres.Members.getContentUri("external", id),
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG,
                SelectionArgs.LOCAL_SONG,
                AudioColumns.ALBUM_KEY + ", " + AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            LocalSong[] songs = new LocalSong[cursor.getCount()];
            if (cursor.moveToFirst()) {
                int ii=0;
                do {
                    songs[ii++] = CursorHelpers.makeLocalSongFromCursor(context, cursor);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return songs;
        }
        return sEmptyLocalSongList;
    }

    @MarkedForRemoval
    @Deprecated
    public static LocalSong[] getLocalSongList(Context context, long[] ids) {
        if (ids == null || ids.length == 0) {
            return sEmptyLocalSongList;
        }
        Cursor c = new OrderPreservingCursor(context, ids);
        LocalSong[] songs = new LocalSong[c.getCount()];
        if (c.getCount() > 0 && c.moveToFirst()) {
            int ii=0;
            do {
                final LocalSong s = CursorHelpers.makeLocalSongFromCursor(context, c);
                songs[ii++] = s;
            } while (c.moveToNext());
        }
        c.close();

//        int ii;
//        for (ii=0;ii<ids.length;ii++) {
//            Timber.d(ids[ii] + " :: "+ songs[ii].songId);
//        }

        return songs;

    }

    /**
     * @param context The {@link Context} to use
     * @param uri The source of the file
     */
    public static void playFile(final Context context, final Uri uri) {
        if (uri == null || sService == null) {
            return;
        }

        // If this is a file:// URI, just use the path directly instead
        // of going through the open-from-filedescriptor codepath.
        String filename;
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            filename = uri.getPath();
        } else {
            filename = uri.toString();
        }

        try {
            sService.stop();
            sService.openFile(filename);
            sService.play();
        } catch (final RemoteException ignored) {
        }
    }


    @Deprecated
    public static void playAll(final Context context, final long[] list, int position,
            final boolean forceShuffle) {
        try {
            playAll(sService, list, position, forceShuffle);
        } catch (Exception ignored) { }
    }

    /**
     * @param list The list of songs to play. (ids must be from musicprovider (recent id)
     * @param position Specify where to start.
     * @param forceShuffle True to force a shuffle, false otherwise.
     */
    public static void playAll(IApolloService service, long[] list, int position, boolean forceShuffle) throws RemoteException {
        if (list.length == 0 || service == null) {
            return;
        }
        if (forceShuffle) {
            service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
        } else {
            service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
        }
        final long currentId = service.getAudioId();
        final int currentQueuePosition = getQueuePosition();
        if (position != -1 && currentQueuePosition == position && currentId == list[position]) {
            final long[] playlist = getQueue();
            if (Arrays.equals(list, playlist)) {
                service.play();
                return;
            }
        }
        if (position < 0) {
            position = 0;
        }
        service.open(list, forceShuffle ? -1 : position);
        service.play();
    }

    @Deprecated
    public static void playAllSongs(Context context, Song[] list, int position, boolean forceShuffle) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        if (list.length == 0 || sService == null) {
            return;
        }
        long[] ids = new long[list.length];
        for (int ii=0; ii<list.length; ii++) {
            //TODO bulk insert?
             ids[ii] = MusicProviderUtil.insertSong(context, list[ii]);
        }
        playAll(context, ids, position, forceShuffle);
    }

    /**
     *
     * @param context
     * @param list long[] containing ids of songs from mediastore
     * @param position
     * @param forceShuffle
     */
    public static void playAllfromMediaStore(Context context, long[] list, int position, boolean forceShuffle) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        if (list == null || list.length == 0 || sService == null) {
            return;
        }
        long[] ids = new long[list.length];
        for (int ii=0; ii<list.length; ii++) {
            ids[ii] = MusicProviderUtil.insertFromMediaStore(context, list[ii]);
        }
        playAll(context, ids, position, forceShuffle);
    }

    /**
     * @param list The list to enqueue.
     */
    @MarkedForRemoval @Deprecated
    public static void playNext(long[] recentslist) {
        if (sService == null) {
            return;
        }
        try {
            sService.enqueue(recentslist, MusicPlaybackService.NEXT);
        } catch (final RemoteException ignored) {
        }
    }

    @MarkedForRemoval @Deprecated
    public static void playNext(Context context, Song[] list) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        if (list.length == 0 || sService == null) {
            return;
        }
        long[] ids = new long[list.length];
        for (int ii=0; ii<list.length; ii++) {
            //TODO bulk insert?
            ids[ii] = MusicProviderUtil.insertSong(context, list[ii]);
        }
        playNext(ids);
    }

    /**
     * @param context The {@link Context} to use.
     */
    public static void shuffleAll(final Context context) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        Cursor cursor = CursorHelpers.makeSongCursor(context);
        final long[] mTrackList = getSongListForCursor(cursor);
        final int position = 0;
        if (mTrackList.length == 0 || sService == null) {
            return;
        }
        try {
            final long[] realTrackList = new long[mTrackList.length];
            for (int ii=0; ii<mTrackList.length; ii++) {
                realTrackList[ii] = MusicProviderUtil.insertFromMediaStore(context, mTrackList[ii]);
            }
            sService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            final long mCurrentId = sService.getAudioId();
            final int mCurrentQueuePosition = getQueuePosition();
            if (mCurrentQueuePosition == position
                    && mCurrentId == realTrackList[position]) {
                final long[] mPlaylist = getQueue();
                if (Arrays.equals(realTrackList, mPlaylist)) {
                    sService.play();
                    return;
                }
            }
            sService.open(realTrackList, -1);
            sService.play();
            cursor.close();
            cursor = null;
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Starts auto shuffle mode
     */
    public static void startPartyShuffle() {
        if (sService != null) {
            try {
                sService.setShuffleMode(MusicPlaybackService.SHUFFLE_AUTO);
            } catch (RemoteException ignored) { }
        }
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name The name of the playlist.
     * @return The ID for a playlist.
     */
    public static final long getIdForPlaylist(final Context context, final String name) {
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
    public static final long getIdForArtist(final Context context, final String name) {
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
    public static final long getIdForAlbum(final Context context, final String albumName,
            final String artistName) {
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
    public static final long createPlaylist(final Context context, final String name) {
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
        context.getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
    }

    /**
     * @param context The {@link Context} to use.
     * @param ids The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(final Context context, final long[] ids, final long playlistid) {
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
        context.getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
    }

    /**
     * Removes a single track from a given playlist
     * @param context The {@link Context} to use.
     * @param id The id of the song to remove.
     * @param playlistId The id of the playlist being removed from.
     */
    public static void removeFromPlaylist(final Context context, final long id,
            final long playlistId) {
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        final ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, Playlists.Members.AUDIO_ID + " = ? ", new String[] {
            Long.toString(id)
        });
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtracksfromplaylist, 1, 1);
        Toast.makeText((Activity)context, message, Toast.LENGTH_LONG).show();
        context.getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
    }

    /**
     * @param context The {@link Context} to use.
     * @param list The list to enqueue.
     */
    public static void addToQueue(final Context context, final long[] list) {
        if (sService == null) {
            return;
        }
        try {
            sService.enqueue(list, MusicPlaybackService.LAST);
            final String message = makeLabel(context, R.plurals.NNNtrackstoqueue, list.length);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } catch (final RemoteException ignored) {
        }
    }

    public static CharSequence addToQueueSilent(final Context context, final long[] list) {
        if (sService == null) {
            return null;
        }
        try {
           sService.enqueue(list, MusicPlaybackService.LAST);
            return makeLabel(context, R.plurals.NNNtrackstoqueue, list.length);
        } catch (final RemoteException ignored) {
        }
        return null;
    }

    public static void addSongsToQueue(Context context, Song[] list) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        if (list.length == 0 || sService == null) {
            return;
        }
        long[] ids = new long[list.length];
        for (int ii=0; ii<list.length; ii++) {
            ids[ii] = MusicProviderUtil.insertSong(context, list[ii]);
        }
        addToQueue(context, ids);
    }

    public static CharSequence addSongsToQueueSilent(Context context, Song[] list) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Stop calling from main thread");
        }
        if (list.length == 0 || sService == null) {
            return null;
        }
        long[] ids = new long[list.length];
        for (int ii=0; ii<list.length; ii++) {
            ids[ii] = MusicProviderUtil.insertSong(context, list[ii]);
        }
        return addToQueueSilent(context, ids);
    }

    /**
     * @param context The {@link Context} to use
     * @param id The song ID.
     */
    public static void setRingtone(final Context context, final long id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (final UnsupportedOperationException ingored) {
            return;
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
                final String message = context.getString(R.string.set_as_ringtone,
                        cursor.getString(2));
                Toast.makeText((Activity)context, message, Toast.LENGTH_LONG).show();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The id of the album.
     * @return The song count for an album.
     */
    public static final int getSongCountForAlbum(final Context context, final long id) {
        if (id == -1) {
            return 0;
        }
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, new String[] {
                    AlbumColumns.NUMBER_OF_SONGS
                }, null, null, null);
        int songCount = 0;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                songCount = cursor.getInt(0);
            }
            cursor.close();
            cursor = null;
        }
        return songCount;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id The id of the album.
     * @return The release date for an album.
     */
    public static final String getReleaseDateForAlbum(final Context context, final long id) {
        if (id == -1) {
            return null;
        }
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
        Cursor cursor = context.getContentResolver().query(uri, new String[] {
                    AlbumColumns.FIRST_YEAR
                }, null, null, null);
        String releaseDate = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                releaseDate = cursor.getString(0);
            }
            cursor.close();
            cursor = null;
        }
        return releaseDate;
    }

    /**
     * @return The path to the currently playing file as {@link String}
     */
    public static Uri getFileUri() {
        try {
            if (sService != null) {
                return sService.getDataUri();
            }
        } catch (final RemoteException ignored) {
        }
        return null;
    }

    public static Uri getArtworkUri() {
        try {
            if (sService != null) {
                return sService.getArtworkUri();
            }
        } catch (final RemoteException ignored) {
        }
        return null;
    }

    /**
     * @param from The index the item is currently at.
     * @param to The index the item is moving to.
     */
    @Deprecated @MarkedForRemoval
    public static void moveQueueItem(final int from, final int to) {
        try {
            if (sService != null) {
                sService.moveQueueItem(from, to);
            } else {
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Toggles the current song as a favorite.
     */
    public static void toggleFavorite() {
        try {
            if (sService != null) {
                sService.toggleFavorite();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @return True if the current song is a favorite, false otherwise.
     */
    public static final boolean isFavorite() {
        try {
            if (sService != null) {
                return sService.isFavorite();
            }
        } catch (final RemoteException ignored) {
        }
        return false;
    }

    public static boolean isFromSDCard() {
        try {
            if (sService != null) {
                return sService.isFromSDCard();
            }
        } catch (final RemoteException ignored) {
        }
        return false;
    }

    /**
     * Plays a user created playlist.
     *
     * @param context The {@link Context} to use.
     * @param playlistId The playlist Id.
     */
    @MarkedForRemoval
    public static void playPlaylist(final Context context, final long playlistId, final boolean forceShuffle) {
        playAllSongs(context, CursorHelpers.getSongsForPlaylist(context, playlistId), 0, forceShuffle);
    }

    /**
     * Plays the last added songs from the past two weeks.
     *
     * @param context The {@link Context} to use
     */
    @MarkedForRemoval
    public static void playLastAdded(final Context context, final boolean forceShuffle) {
        playAllSongs(context, CursorHelpers.getSongsForLastAdded(context), 0, forceShuffle);
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public static void refresh() {
        try {
            if (sService != null) {
                sService.refresh();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Queries {@link RecentStore} for the last album played by an artist
     *
     * @param context The {@link Context} to use
     * @param artistName The artist name
     * @return The last album name played by an artist
     */
    public static final String getLastAlbumForArtist(final Context context, final String artistName) {
        return RecentStore.getInstance(context).getAlbumName(artistName);
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    public static void seek(final long position) {
        if (sService != null) {
            try {
                sService.seek(position);
            } catch (final RemoteException ignored) {
            }
        }
    }

    /**
     * @return The current position time of the track
     */
    public static final long position() {
        if (sService != null) {
            try {
                return sService.position();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static final long duration() {
        if (sService != null) {
            try {
                return sService.duration();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @param position The position to move the queue to
     */
    @Deprecated @MarkedForRemoval
    public static void setQueuePosition(final int position) {
        if (sService != null) {
            try {
                sService.setQueuePosition(position);
            } catch (final RemoteException ignored) {
            }
        }
    }

    /**
     * Clears the qeueue
     */
    public static void clearQueue() {
        try {
            sService.removeTracks(0, Integer.MAX_VALUE);
        } catch (final RemoteException ignored) {
        }
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
    public static CharSequence deleteTracks(final Context context, final long[] list) {
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
            // Step 1: Remove selected tracks from the current playlist, as well
            // as from the album art cache
            if (c.moveToFirst()) {
                do {
                    long id = MusicProviderUtil.getRecentId(context, c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID)));
                    if (id >= 0) {
                        // Remove from current playlist
                        removeQueueItem(id);
                        // Remove from the favorites playlist
//                FavoritesStore.getInstance(context).removeItem(id);
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
        }
        // We deleted a number of tracks, which could affect any number of
        // things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        context.getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
        context.getContentResolver().notifyChange(MusicProvider.GENRES_URI, null);
        // Notify the lists to update
        refresh();

        final String message = makeLabel(context, R.plurals.NNNtracksdeleted, list.length);

        return message;
    }
}
