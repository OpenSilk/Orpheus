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

import android.content.SharedPreferences;
import android.util.Log;

import com.andrew.apollo.utils.Lists;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.andrew.apollo.PlaybackConstants.*;


/**
 * Created by drew on 4/22/15.
 */
@Singleton
public class PlaybackQueue {
    /**
     * Keeps a mapping of the track history
     */
    private final LinkedList<Integer> mHistory = Lists.newLinkedList();
    private final LinkedList<Integer> mAutoHistory = Lists.newLinkedList();
    /**
     * Used to shuffle the tracks
     */
    private final Shuffler mShuffler = new Shuffler();

    private final PlaybackSettings mSettings;

    @Inject
    public PlaybackQueue(PlaybackSettings mSettings) {
        this.mSettings = mSettings;
    }

    private boolean mQueueIsSaveable;

    private int mPlayListLen = 0;

    private int mPlayPos = -1;

    private int mNextPlayPos = -1;

    private long mLastKnowPosition = 0;

    private int mOpenFailedCounter = 0;

    private int mMediaMountedCount = 0;

    private long[] mPlayList = null;

    private long[] mAutoShuffleList = null;

    private int mShuffleMode = SHUFFLE_NONE;

    private int mRepeatMode = REPEAT_NONE;

    public boolean isSaveable() {
        return mQueueIsSaveable;
    }

    public int playlistLength() {
        return mPlayListLen;
    }

    public void setPlaylistLength(int len) {
        mPlayListLen = len;
    }

    public long[] playlist() {
        return mPlayList;
    }

    public void set(int loc, long id) {
        mPlayList[loc] = id;
    }

    public List<Integer> history() {
        return mHistory;
    }

    public List<Integer> autoHistory() {
        return mAutoHistory;
    }

    public int playPos() {
        return mPlayPos;
    }

    public void setPlayPos(int pos) {
        mPlayPos = pos;
    }

    public long peekCurrent() {
        return mPlayList[mPlayPos];
    }

    public int shuffleMode() {
        return mShuffleMode;
    }

    public void setShuffleMode(int mode) {
        mShuffleMode = mode;
    }

    public int repeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(int mode) {
        mRepeatMode = mode;
    }

    public long peekNext() {
        return mPlayList[mNextPlayPos];
    }

    public void ensurePlayListCapacity(final int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            final long[] newlist = new long[size * 2];
            if (mPlayList != null) {
                final int len = mPlayList.length;
                System.arraycopy(mPlayList, 0, newlist, 0, len);
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }
}
