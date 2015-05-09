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

package org.opensilk.music.playback;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackQueue {

    final PlaybackPreferences mSettings;
    final List<Uri> mQueue = new ArrayList<>();

    int mCurrentPos = -1;
    QueueChangeListener mListener;
    Handler mCallbackHandler;

    @Inject
    public PlaybackQueue(
            PlaybackPreferences mSettings
    ) {
        this.mSettings = mSettings;
    }

    @DebugLog
    public void load() {
        mQueue.clear();
        mQueue.addAll(mSettings.getQueue());
        mCurrentPos = mSettings.getInt(PlaybackPreferences.CURRENT_POS, 0);
        if (mQueue.isEmpty()) {
            mCurrentPos = -1;
        } else if (mCurrentPos >= mQueue.size()) {
            mCurrentPos = 0; //just start over
        }
    }

    public void save() {
        final List<Uri> q = new ArrayList<>(mQueue);
        final int pos = mCurrentPos;
        //Use async to avoid making new thread
        new AsyncTask<Object, Void, Void>() {
            @Override
            @DebugLog
            protected Void doInBackground(Object... params) {
                mSettings.saveQueue(q);
                if (pos != -1) {
                    mSettings.putInt(PlaybackPreferences.CURRENT_POS, pos);
                }
                return null;
            }
        }.execute();
    }

    public void addNext(List<Uri> list) {
        if (mQueue.isEmpty() /*|| mCurrentPos < 0*/) {
            mQueue.addAll(0, list);
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else if (mCurrentPos >= mQueue.size()) {
            //Should never happen
            mCurrentPos = mQueue.size() - 1;
            mQueue.addAll(mQueue.size(), list);
            notifyCurrentPosChanged();
        } else {
            mQueue.addAll(mCurrentPos + 1, list);
            notifyQueueChanged();
        }
    }

    public void addEnd(List<Uri> list) {
        mQueue.addAll(list);
        notifyQueueChanged();
    }

    public void replace(List<Uri> list) {
        replace(list, 0);
    }

    public void replace(List<Uri> list, int startpos) {
        mQueue.clear();
        if (list.isEmpty()) {
            mCurrentPos = -1;
            notifyCurrentPosChanged();
        } else {
            mQueue.addAll(list);
            if (startpos == -1) {
                mCurrentPos = -1;
                shuffle();
            } else {
                mCurrentPos = clamp(startpos);
                notifyCurrentPosChanged();
            }
        }
    }

    public void remove(List<Uri> list) {
        if (list.isEmpty()) {
            return;
        }
        Uri current = null;
        if (mCurrentPos > 0 && mCurrentPos < mQueue.size()) {
            current = mQueue.get(mCurrentPos);
        }
        mQueue.removeAll(list);
        if (mQueue.isEmpty()) {
            mCurrentPos = -1;
            notifyCurrentPosChanged();
        } else if (current != null) {
            int newPos = mQueue.indexOf(current);
            if (newPos >= 0) {
                //We still have the current, update pointer
                mCurrentPos = newPos;
                notifyQueueChanged();
            } else {
                //get the next or loop back to start
                mCurrentPos = getNextPos();
                notifyCurrentPosChanged();
            }
        } else {
            //idk just start at top
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        }
    }

    public void remove(Uri uri) {
        remove(mQueue.indexOf(uri));
    }

    public void remove(int pos) {
        if (pos < 0 || pos >= mQueue.size()) {
            return;
        }
        mQueue.remove(pos);
        if (pos == mCurrentPos) {
            //same as wenttonext, but we also need to reload
            //the current so we notify current changed
            if (++mCurrentPos >= mQueue.size()) {
                mCurrentPos = 0;
            }
            notifyCurrentPosChanged();
        } else {
            notifyQueueChanged();
        }
    }

    public void moveItem(Uri uri, int to) {
        if (uri == null) {
            return;
        }
        if (mQueue.isEmpty()) {
            return;
        }
        Uri current = mQueue.get(mCurrentPos);
        mQueue.remove(uri);
        if (to > mQueue.size()) {
            to = mQueue.size();
        }
        mQueue.add(to, uri);
        if (current == null) {
            //should never happen
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else if (current.equals(uri)) {
            mCurrentPos = to;
            notifyCurrentPosChanged();
        } else {
            notifyQueueChanged();
        }
    }

    public void moveItem(int from, int to) {
        moveItem(mQueue.get(from), to);
    }

    public void clear() {
        mQueue.clear();
        mCurrentPos = -1;
        notifyCurrentPosChanged();
    }

    public void goToItem(int pos) {
        mCurrentPos = clamp(pos);
        notifyCurrentPosChanged();
    }

    public void wentToNext() {
        if (++mCurrentPos >= mQueue.size()) {
            mCurrentPos = 0;
        }
        notifyWentToNext();
    }

    public int getNextPos() {
        if (mCurrentPos + 1 >= mQueue.size()) {
            return 0;
        } else {
            return mCurrentPos + 1;
        }
    }

    public int getPrevious() {
        if (mCurrentPos - 1 < 0) {
            return mQueue.size() - 1;
        }
        return mCurrentPos - 1;
    }

    public int getCurrentPos() {
        return mCurrentPos;
    }

    public void shuffle() {
        if (mCurrentPos == -1) {
            Collections.shuffle(mQueue);
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else {
            Uri current = mQueue.get(mCurrentPos);
            Collections.shuffle(mQueue);
            int idx = mQueue.indexOf(current);
            //Make sure we dont change the current position
            if (idx != mCurrentPos) {
                mQueue.remove(idx);
                mQueue.add(mCurrentPos, current);
            }
            notifyQueueChanged();
        }
    }

    public boolean notEmpty() {
        return !mQueue.isEmpty();
    }

    public Uri getCurrentUri() {
        return mQueue.get(mCurrentPos);
    }

    public Uri getNextUri() {
        return mQueue.get(getNextPos());
    }

    int clamp(int pos) {
        return (pos < 0) ? 0 : (pos >= mQueue.size()) ? (mQueue.size() - 1) : pos;
    }

    void notifyCurrentPosChanged() {
        if (mListener != null) {
            if (mCallbackHandler != null) {
                final QueueChangeListener l = mListener;
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        l.onCurrentPosChanged();
                    }
                });
            } else {
                mListener.onCurrentPosChanged();
            }
        }
    }

    void notifyQueueChanged() {
        if (mListener != null) {
            if (mCallbackHandler != null) {
                final QueueChangeListener l = mListener;
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        l.onQueueChanged();
                    }
                });
            } else {
                mListener.onQueueChanged();
            }
        }
    }

    void notifyWentToNext() {
        if (mListener != null) {
            if (mCallbackHandler != null) {
                final QueueChangeListener l = mListener;
                mCallbackHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        l.wentToNext();
                    }
                });
            } else {
                mListener.wentToNext();
            }
        }
    }

    public synchronized void setListener(QueueChangeListener l, Handler h) {
        mListener = l;
        mCallbackHandler = h;
    }

    public interface QueueChangeListener {
        void onCurrentPosChanged();
        void onQueueChanged();
        void wentToNext();
    }
}
