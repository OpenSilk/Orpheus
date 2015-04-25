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

package com.andrew.apollo;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.andrew.apollo.PlaybackConstants.REPEAT_ALL;
import static com.andrew.apollo.PlaybackConstants.REPEAT_CURRENT;
import static com.andrew.apollo.PlaybackConstants.REPEAT_NONE;
import static com.andrew.apollo.PlaybackConstants.SHUFFLE_AUTO;
import static com.andrew.apollo.PlaybackConstants.SHUFFLE_NONE;
import static com.andrew.apollo.PlaybackConstants.SHUFFLE_NORMAL;

/**
 * Created by drew on 4/22/15.
 */
@Singleton
public class PlaybackSettings {
    public static final String NAME = "Service";

    /**
     * Used to save the queue as reverse hexadecimal numbers, which we can
     * generate faster than normal decimal or hexadecimal numbers, which in
     * turn allows us to save the playlist more often without worrying too
     * much about performance
     */
    private static final char HEX_DIGITS[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private final PlaybackService mService;

    // We use this to distinguish between different cards when saving/restoring
    // playlists
    private int mCardId;

    @Inject
    public PlaybackSettings(PlaybackService service) {
        mService = service;
    }

    private SharedPreferences getPrefs() {
        return mService.getSharedPreferences(NAME, Context.MODE_MULTI_PROCESS);
    }

    /**
     * Saves the queue
     *
     * @param full True if the queue is full
     */
    private void saveQueue(final boolean full) {
        if (!mService.getQueue().isSaveable()) {
            return;
        }
        final PlaybackQueue queue = mService.getQueue();
        final SharedPreferences.Editor editor = getPrefs().edit();
        if (full) {
            final StringBuilder q = new StringBuilder();
            long[] mPlayList = queue.playlist();
            int len = queue.playlistLength();
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n < 0) {
                    continue;
                } else if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int)(n & 0xf);
                        n >>>= 4;
                        q.append(HEX_DIGITS[digit]);
                    }
                    q.append(";");
                }
            }
            editor.putString("queue", q.toString());
            editor.putInt("cardid", mCardId);
            // save shuffle history
            List<Integer> mHistory = queue.history();
            len = mHistory.size();
            if (len > 0) {
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            final int digit = n & 0xf;
                            n >>>= 4;
                            q.append(HEX_DIGITS[digit]);
                        }
                        q.append(";");
                    }
                }
                editor.putString("history", q.toString());
            } else {
                editor.remove("history");
            }
            // save autoshuffle history
            List<Integer> mAutoHistory = queue.autoHistory();
            len = mAutoHistory.size();
            if (len > 0) {
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mAutoHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            final int digit = n & 0xf;
                            n >>>= 4;
                            q.append(HEX_DIGITS[digit]);
                        }
                        q.append(";");
                    }
                }
                editor.putString("autohistory", q.toString());
            } else {
                editor.remove("autohistory");
            }
        }
        editor.putInt("curpos", queue.playPos());
        /*
        final IMusicPlayer player = mService.getPlayer();
        if (player != null && player.isInitialized()) {
            editor.putLong("seekpos", player.position());
        }
        */
        editor.putInt("repeatmode", mService.getQueue().repeatMode());
        editor.putInt("shufflemode", mService.getQueue().shuffleMode());
        editor.putInt("schemaversion", PREF_VERSION);
        editor.apply();
    }

    /**
     * Increment value when pref schema changes
     */
    private static final int PREF_VERSION = 2;

    /**
     * Reloads the queue as the user left it the last time they stopped using
     * Apollo
     */
    private void reloadQueue() {
        final SharedPreferences mPreferences = getPrefs();
        int ver = mPreferences.getInt("schemaversion", 0);
        if (ver < PREF_VERSION) {
            return;
        }

        String q = null;
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            q = mPreferences.getString("queue", "");
        }

        final PlaybackQueue queue = mService.getQueue();

        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                final char c = q.charAt(i);
                if (c == ';') {
                    queue.ensurePlayListCapacity(plen + 1);
                    queue.set(plen, n);
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += c - '0' << shift;
                    } else if (c >= 'a' && c <= 'f') {
                        n += 10 + c - 'a' << shift;
                    } else {
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            queue.setPlaylistLength(plen);
            final int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= queue.playlistLength()) {
                queue.setPlaylistLength(0);
                return;
            }
            queue.setPlayPos(pos);

//            closeCursor();
//            mOpenFailedCounter = 20;
//            openCurrentAndNext();
//            final IMusicPlayer player = getPlayer();
//            if (player != null && !player.isInitialized()) {
//                mPlayListLen = 0;
//                return;
//            }
//
//            final long seekpos = mPreferences.getLong("seekpos", 0);
//            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
//
//            if (D) {
//                Log.d(TAG, "restored queue, currently at position "
//                        + position() + "/" + duration()
//                        + " (requested " + seekpos + ")");
//            }

            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            queue.setRepeatMode(repmode);

            // restore shuffle history
            q = mPreferences.getString("history", "");
            qlen = q != null ? q.length() : 0;
            if (qlen > 1) {
                plen = 0;
                n = 0;
                shift = 0;
                List<Integer> history = queue.history();
                history.clear();
                for (int i = 0; i < qlen; i++) {
                    final char c = q.charAt(i);
                    if (c == ';') {
                        if (n >= queue.playlistLength()) {
                            history.clear();
                            break;
                        }
                        history.add(n);
                        n = 0;
                        shift = 0;
                    } else {
                        if (c >= '0' && c <= '9') {
                            n += c - '0' << shift;
                        } else if (c >= 'a' && c <= 'f') {
                            n += 10 + c - 'a' << shift;
                        } else {
                            history.clear();
                            break;
                        }
                        shift += 4;
                    }
                }
            }
            // restore auto shuffle history
            q = mPreferences.getString("autohistory", "");
            qlen = q != null ? q.length() : 0;
            if (qlen > 1) {
                plen = 0;
                n = 0;
                shift = 0;
                List<Integer> autoHistory = queue.autoHistory();
                autoHistory.clear();
                for (int i = 0; i < qlen; i++) {
                    final char c = q.charAt(i);
                    if (c == ';') {
//                        if (n >= queue.playlistLength()) {
//                            autoHistory.clear();
//                            break;
//                        }
                        autoHistory.add(n);
                        n = 0;
                        shift = 0;
                    } else {
                        if (c >= '0' && c <= '9') {
                            n += c - '0' << shift;
                        } else if (c >= 'a' && c <= 'f') {
                            n += 10 + c - 'a' << shift;
                        } else {
                            autoHistory.clear();
                            break;
                        }
                        shift += 4;
                    }
                }
            }
            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
//            if (shufmode == SHUFFLE_AUTO) {
//                if (!makeAutoShuffleList()) {
//                    shufmode = SHUFFLE_NONE;
//                }
//            }
            queue.setShuffleMode(shufmode);
        }
    }

    private int getCardId() {
        final ContentResolver resolver = mService.getContentResolver();
        Cursor cursor = resolver.query(Uri.parse("content://media/external/fs_id"), null, null,
                null, null);
        int mCardId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            mCardId = cursor.getInt(0);
            cursor.close();
            cursor = null;
        }
        return mCardId;
    }
}
