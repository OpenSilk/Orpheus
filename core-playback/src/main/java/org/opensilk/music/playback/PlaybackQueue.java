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

import android.media.session.MediaSession.QueueItem;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import org.apache.commons.lang3.StringUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackQueue {

    final PlaybackPreferences mSettings;
    final MediaMetadataHelper mMetaHelper;
    final List<Uri> mQueue = new ArrayList<>();
    List<QueueItem> mQueueMeta;

    int mCurrentPos = -1;
    QueueChangeListener mListener;
    Handler mCallbackHandler;

    @Inject
    public PlaybackQueue(
            PlaybackPreferences mSettings,
            MediaMetadataHelper mMetaHelper
    ) {
        this.mSettings = mSettings;
        this.mMetaHelper = mMetaHelper;
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
        updateQueueMeta();
    }

    public static class Snapshot {
        public final List<Uri> q;
        public final int pos;
        public Snapshot(List<Uri> q, int pos) {
            this.q = new ArrayList<>(q);
            this.pos = pos;
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(mQueue, mCurrentPos);
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
        if (mRepeatCurrent) {
            return mCurrentPos;
        } else if (mCurrentPos + 1 >= mQueue.size()) {
            return 0;
        } else {
            return mCurrentPos + 1;
        }
    }

    public int getPrevious() {
        if (mRepeatCurrent) {
            return mCurrentPos;
        } else if (mCurrentPos - 1 < 0) {
            return mQueue.size() - 1;
        } else {
            return mCurrentPos - 1;
        }
    }

    public int getCurrentPos() {
        return mCurrentPos;
    }

    public List<Uri> get() {
        return new ArrayList<>(mQueue);
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

    boolean mRepeatCurrent;

    public void toggleRepeat() {
        mRepeatCurrent = !mRepeatCurrent;
        //TODO notify
    }

    int clamp(int pos) {
        return (pos < 0) ? 0 : (pos >= mQueue.size()) ? (mQueue.size() - 1) : pos;
    }

    public List<QueueItem> getQueueItems() {
        if (mQueueMeta == null) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(mQueueMeta);
        }
    }

    public void updateQueueMeta() {
        List<QueueItem> qm;
        if (mQueueMeta == null) {
            qm = new ArrayList<>(mQueue.size());
        } else if (mQueue.size() < mQueueMeta.size()) {
            qm = mQueueMeta.subList(0, mQueue.size() - 1);
        } else {
            qm = new ArrayList<>(mQueueMeta);
        }
        ListIterator<Uri> qi = mQueue.listIterator();
        ListIterator<QueueItem> qmi = qm.listIterator();
        //if this was a change towards the end the start of the lists will be the same
        while (qi.hasNext() && qmi.hasNext()) {
            final Uri uri = qi.next();
            final QueueItem item = qmi.next();
            if (!StringUtils.equals(uri.toString(), item.getDescription().getMediaId())) {
                //rewind
                qi.previous();
                qmi.previous();
                break;
            }
        }
        //loop the remaining queue and update meta to match
        while (qi.hasNext()) {
            final Uri uri = qi.next();
            QueueItem queueItem = null;
            if (mQueueMeta != null) {
                //check if item exits in another position.
                for (QueueItem item2 : mQueueMeta) {
                    if (uri.toString().equals(item2.getDescription().getMediaId())) {
                        queueItem = new QueueItem(item2.getDescription(), qi.previousIndex());
                        break;
                    }
                }
            }
            if (queueItem == null) {
                //new item, fetch the meta from the provider
                //todo it would be great to fetch all at once
                queueItem = mMetaHelper.buildQueueItem(uri, qi.previousIndex());
            }
            if (queueItem == null) {
                //item was removed externally todo this might fuckup currentpos
                qi.remove();
            } else {
                if (qmi.hasNext()) {
                    //replace next with the new value
                    qmi.next();
                    qmi.set(queueItem);
                } else {
                    //add new value
                    qmi.add(queueItem);
                }
            }
        }
        mCurrentPos = clamp(mCurrentPos); //safety, todo check proper
        mQueueMeta = qm;

        if (mQueue.size() != mQueueMeta.size()) {
            Timber.e("Queues don't match");
        }
        qi = mQueue.listIterator();
        qmi = mQueueMeta.listIterator();
        int ii=0;
        while (qi.hasNext() && qmi.hasNext()) {
            Uri uri = qi.next();
            QueueItem queueItem = qmi.next();
            Timber.v("%d -> %s\n%d -> %s", ii++, uri, queueItem.getQueueId(), queueItem.getDescription().getMediaId());
        }
    }

    void notifyCurrentPosChanged() {
        updateQueueMeta();
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
        updateQueueMeta();
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
