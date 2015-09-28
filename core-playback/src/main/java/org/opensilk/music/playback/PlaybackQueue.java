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

import android.media.MediaDescription;
import android.media.session.MediaSession.QueueItem;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.service.PlaybackService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackQueue {

    final IndexClient mIndexClient;
    final PlaybackService mService;
    final List<Uri> mQueue = new ArrayList<>();
    List<QueueItem> mQueueMeta;

    int mCurrentPos = -1;
    int mRepeatMode = PlaybackConstants.REPEAT_ALL;
    int mShuffleMode = PlaybackConstants.SHUFFLE_NONE;
    QueueChangeListener mListener;
    final AtomicLong mIdGenerator = new AtomicLong(1);
    Subscription mLookupSub;

    @Inject
    public PlaybackQueue(
            IndexClient mIndexClient,
            PlaybackService mService
    ) {
        this.mIndexClient = mIndexClient;
        this.mService = mService;
    }

    @DebugLog
    public void load() {
        mQueue.clear();
        mQueue.addAll(mIndexClient.getLastQueue());
        mCurrentPos = mIndexClient.getLastQueuePosition();
        if (mQueue.isEmpty()) {
            mCurrentPos = -1;
        } else if (mCurrentPos >= mQueue.size()) {
            mCurrentPos = 0; //just start over
        }
        mRepeatMode = mIndexClient.getLastQueueRepeateMode();
        mShuffleMode = mIndexClient.getLastQueueShuffleMode();
        notifyCurrentPosChanged();
    }

    public static class Snapshot {
        public final List<Uri> q;
        public final int pos;
        public final int repeat;
        public final int shuffle;
        public Snapshot(List<Uri> q, int pos, int repeat, int shuffle) {
            this.q = new ArrayList<>(q);
            this.pos = pos;
            this.repeat = repeat;
            this.shuffle = shuffle;
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(mQueue, mCurrentPos, mRepeatMode, mShuffleMode);
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
        switch (mShuffleMode) {
            case PlaybackConstants.SHUFFLE_NORMAL:
                shuffle();
                break;
            default:
                notifyQueueChanged();
                break;
        }
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
            mCurrentPos = clamp(startpos);
            switch (mShuffleMode) {
                case PlaybackConstants.SHUFFLE_NORMAL:
                    shuffle();
                    break;
                default:
                    notifyCurrentPosChanged();
                    break;
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
                switch (mRepeatMode) {
                    case PlaybackConstants.REPEAT_ALL:
                        mCurrentPos = 0;
                        notifyCurrentPosChanged();
                        return; //skip extra notify
                    default:
                        mCurrentPos = -1;
                        notifyQueueChanged();
                        break;
                }
            }
        }
        notifyQueueChanged();
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

    public int getPosOfId(long id) {
        int pos = 0;
        boolean found = false;
        for (QueueItem qi : mQueueMeta) {
            if (qi.getQueueId() == id) {
                found = true;
                break;
            }
            pos++;
        }
        return found ? pos : -1;
    }

    public void goToItem(int pos) {
        mCurrentPos = clamp(pos);
        notifyCurrentPosChanged();
    }

    public void moveToNext() {
        if (++mCurrentPos >= mQueue.size()) {
            switch (mRepeatMode) {
                case PlaybackConstants.REPEAT_ALL:
                    mCurrentPos = 0;
                    notifyWentToNext();
                    break;
                default:
                    mCurrentPos = -1;
                    break;
            }
        }
    }

    /**
     * negative means cant go forward
     */
    public int getNextPos() {
        switch (mRepeatMode) {
            case PlaybackConstants.REPEAT_CURRENT:
                return mCurrentPos;
            case PlaybackConstants.REPEAT_ALL:
            case PlaybackConstants.REPEAT_NONE:
            default:
                if (mCurrentPos + 1 >= mQueue.size()) {
                    switch (mRepeatMode) {
                        case PlaybackConstants.REPEAT_ALL:
                            return 0;
                        default:
                            return -1;
                    }
                } else {
                    return mCurrentPos + 1;
                }
        }
    }

    /**
     * negative value means cant go back
     */
    public int getPrevious() {
        switch (mRepeatMode) {
            case PlaybackConstants.REPEAT_CURRENT:
                return mCurrentPos;
            case PlaybackConstants.REPEAT_ALL:
                if (mCurrentPos - 1 < 0) {
                    return mQueue.size() - 1;
                } else {
                    return mCurrentPos - 1;
                }
            case PlaybackConstants.REPEAT_NONE:
            default:
                return (mCurrentPos - 1);
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

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void toggleRepeat() {
        int newMode = mRepeatMode;
        switch (mRepeatMode) {
            case PlaybackConstants.REPEAT_NONE:
                newMode = PlaybackConstants.REPEAT_ALL;
                break;
            case PlaybackConstants.REPEAT_ALL:
                newMode = PlaybackConstants.REPEAT_CURRENT;
                break;
            case PlaybackConstants.REPEAT_CURRENT:
                newMode = PlaybackConstants.REPEAT_NONE;
                break;
        }
        setRepeatMode(newMode);
    }

    public void setRepeatMode(int newMode) {
        if (mRepeatMode == newMode) {
            return;
        }
        mRepeatMode = newMode;
        notifyQueueChanged();
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    public void toggleShuffle() {
        int newMode = mShuffleMode;
        switch (mShuffleMode) {
            case PlaybackConstants.SHUFFLE_NONE:
                newMode = PlaybackConstants.SHUFFLE_NORMAL;
                break;
            case PlaybackConstants.SHUFFLE_NORMAL:
                newMode = PlaybackConstants.SHUFFLE_NONE;
                break;
        }
        setShuffleMode(newMode);
    }

    public void setShuffleMode(int newMode) {
        if (mShuffleMode == newMode) {
            return;
        }
        mShuffleMode = newMode;
        switch (newMode) {
            case PlaybackConstants.SHUFFLE_NONE:
                //todo or how?
                break;
            case PlaybackConstants.SHUFFLE_NORMAL:
                shuffle();
                break;
        }
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

    public QueueItem getCurrentQueueItem() {
        if (mQueueMeta == null || mQueueMeta.size() < mCurrentPos || mCurrentPos < 0) {
            return null;
        } else {
            return mQueueMeta.get(mCurrentPos);
        }
    }

    public void updateQueueMeta(List<MediaDescriptionCompat> descriptions) {
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
                        queueItem = item2;
                        break;
                    }
                }
            }
            if (queueItem == null) {
                //new item
                queueItem = makeNewQueueItem(uri, descriptions);
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

        if (BuildConfig.DEBUG) {
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
    }

    QueueItem makeNewQueueItem(Uri uri, List<MediaDescriptionCompat> descriptions) {
        for (MediaDescriptionCompat desc : descriptions) {
            if (uri.equals(desc.getMediaUri())) {
                return new QueueItem((MediaDescription)desc.getMediaDescription(), mIdGenerator.incrementAndGet());
            }
        }
        return null;
    }

    void notifyCurrentPosChanged() {
        if (mLookupSub != null) {
            mLookupSub.unsubscribe();
        }
        mLookupSub = mIndexClient.getDescriptions(mQueue)
                .observeOn(mService.getScheduler())
                .subscribe(new Action1<List<MediaDescriptionCompat>>() {
                    @Override
                    public void call(List<MediaDescriptionCompat> mediaDescriptionCompats) {
                        updateQueueMeta(mediaDescriptionCompats);
                        if (mListener != null) {
                            mListener.onCurrentPosChanged();
                        }
                        mLookupSub = null;
                    }
                });
    }

    void notifyQueueChanged() {
        if (mLookupSub != null) {
            mLookupSub.unsubscribe();
        }
        mLookupSub = mIndexClient.getDescriptions(mQueue)
                .observeOn(mService.getScheduler())
                .subscribe(new Action1<List<MediaDescriptionCompat>>() {
                    @Override
                    public void call(List<MediaDescriptionCompat> mediaDescriptionCompats) {
                        updateQueueMeta(mediaDescriptionCompats);
                        if (mListener != null) {
                            mListener.onQueueChanged();
                        }
                        mLookupSub = null;
                    }
                });
    }

    void notifyWentToNext() {
        if (mListener != null) {
            mListener.onMovedToNext();
        }
    }

    public synchronized void setListener(QueueChangeListener l) {
        mListener = l;
    }

    public interface QueueChangeListener {
        void onCurrentPosChanged();
        void onQueueChanged();
        void onMovedToNext();
    }
}
