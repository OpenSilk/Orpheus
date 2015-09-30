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
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;

import org.apache.commons.lang3.ArrayUtils;
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
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackQueue {

    private final IndexClient mIndexClient;
    private final PlaybackService mService;
    private final ArrayList<Uri> mQueue = new ArrayList<>();
    private final ArrayList<QueueItem> mQueueMeta = new ArrayList<>();

    private int mCurrentPos = -1;
    private int mRepeatMode = PlaybackConstants.REPEAT_ALL;
    private int mShuffleMode = PlaybackConstants.SHUFFLE_NONE;
    private QueueChangeListener mListener;
    protected final AtomicLong mIdGenerator = new AtomicLong(1);
    private Subscription mLookupSub;

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
        int pos = mIndexClient.getLastQueuePosition();
        if (pos >= 0) {
            mCurrentPos = pos;
        }
        if (mQueue.isEmpty()) {
            mCurrentPos = -1;
        } else if (mCurrentPos >= mQueue.size()) {
            mCurrentPos = 0; //just start over
        }
        int rep = mIndexClient.getLastQueueRepeatMode();
        switch (rep) {
            case PlaybackConstants.REPEAT_NONE:
            case PlaybackConstants.REPEAT_CURRENT:
            case PlaybackConstants.REPEAT_ALL:
                mRepeatMode = rep;
                break;
            default:
                break;
        }
        int shuf = mIndexClient.getLastQueueShuffleMode();
        switch (shuf) {
            case PlaybackConstants.SHUFFLE_NONE:
            case PlaybackConstants.SHUFFLE_NORMAL:
                mShuffleMode = shuf;
                break;
            default:
                break;
        }
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
        //We don't shuffle these
        if (mQueue.isEmpty() || mCurrentPos == -1) {
            mQueue.addAll(0, list);
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else {
            mQueue.addAll(mCurrentPos + 1, list);
            notifyQueueChanged();
        }
    }

    public void addEnd(List<Uri> list) {
        if (mQueue.isEmpty()) {
            mQueue.addAll(list);
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else {
            int oldsize = mQueue.size();
            mQueue.addAll(list);
            if (isShuffleOn()) {
                shuffle();
            } else if (mCurrentPos < 0) {
                mCurrentPos = oldsize;
                notifyCurrentPosChanged();
            } else {
                notifyQueueChanged();
            }
        }
    }

    public void replace(List<Uri> list) {
        replace(list, 0);
    }

    public void replace(List<Uri> list, int startpos) {
        int oldsize = mQueue.size();
        mQueue.clear();
        if (list.isEmpty()) {
            mCurrentPos = -1;
            notifyCurrentPosChanged();
        } else {
            mQueue.addAll(list);
            if (mQueue.size() * 2 < oldsize) {
                mQueue.trimToSize(); //Trim if much smaller
            }
            if (isShuffleOn()) {
                mCurrentPos = 0;//no need to keep position
                randomizeQueue();
            } else {
                mCurrentPos = clamp(startpos);
            }
            notifyCurrentPosChanged();
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
                //play whatevers next
                mCurrentPos = clamp(mCurrentPos);
                notifyCurrentPosChanged();
            }
        } else {
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        }
    }

    public void remove(Uri uri) {
        remove(Collections.singletonList(uri));
    }

    public void remove(int pos) {
        if (pos < 0 || pos >= mQueue.size()) {
            return;
        }
        remove(mQueue.get(pos));
    }

    public void moveItem(Uri uri, int to) {
        if (uri == null || mQueue.isEmpty()) {
            return;
        }
        Uri current = null;
        if (mCurrentPos > 0 && mCurrentPos < mQueue.size()) {
            current = mQueue.get(mCurrentPos);
        }
        mQueue.get(mCurrentPos);
        mQueue.remove(uri);
        if (to > mQueue.size()) {
            to = mQueue.size();
        }
        mQueue.add(to, uri);
        if (current == null) {
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else {
            int idx = mQueue.indexOf(current);
            if (mCurrentPos != idx) {
                mCurrentPos = idx;
            }
            notifyQueueChanged();
        }
    }

    public void moveItem(int from, int to) {
        if (from == to || from < 0 || from > mQueue.size()) {
            return;
        }
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
        moveToNext(true);
    }

    private void moveToNext(boolean notify) {
        //if were repeating the current or previously went off the end we dont move
        if (mRepeatMode != PlaybackConstants.REPEAT_CURRENT && mCurrentPos != -1) {
            if (++mCurrentPos >= mQueue.size()) {
                //we went of the end, shall we loop back?
                if (mRepeatMode == PlaybackConstants.REPEAT_ALL) {
                    mCurrentPos = 0;
                } else {
                    mCurrentPos = -1;
                }
            }
        }
        if (notify) {
            notifyWentToNext();
        }
    }

    /**
     * negative means cant go forward
     */
    public int getNextPos() {
        if (mRepeatMode == PlaybackConstants.REPEAT_CURRENT) {
            return mCurrentPos;
        } else if (mCurrentPos + 1 >= mQueue.size()) {
            if (mRepeatMode == PlaybackConstants.REPEAT_ALL) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return mCurrentPos + 1;
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

    private void shuffle() {
        if (mCurrentPos < 0 || mCurrentPos >= mQueue.size()) {
            randomizeQueue();
            mCurrentPos = 0;
            notifyCurrentPosChanged();
        } else {
            Uri current = mQueue.get(mCurrentPos);
            randomizeQueue();
            int idx = mQueue.indexOf(current);
            if (mCurrentPos != idx) {
                mCurrentPos = idx;
            }
            notifyQueueChanged();
        }
    }

    private void randomizeQueue() {
        Collections.shuffle(mQueue);
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

    //exposed for testing
    void setRepeatMode(int newMode) {
        if (mRepeatMode == newMode) {
            return;
        }
        mRepeatMode = newMode;
        notifyQueueChanged();
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    private boolean isShuffleOn() {
        return mShuffleMode == PlaybackConstants.SHUFFLE_NORMAL;
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

    //exposed for testing
    void setShuffleMode(int newMode) {
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

    private int clamp(int pos) {
        return (pos < 0) ? 0 : (pos >= mQueue.size()) ? (mQueue.size() - 1) : pos;
    }

    public List<QueueItem> getQueueItems() {
        return new ArrayList<>(mQueueMeta);
    }

    public @Nullable QueueItem getCurrentQueueItem() {
        if (mQueueMeta.isEmpty()
                || mQueueMeta.size() < mCurrentPos
                || mCurrentPos < 0) {
            return null;
        } else {
            return mQueueMeta.get(mCurrentPos);
        }
    }

    @DebugLog
    private void updateQueueMeta(List<MediaDescription> descriptions) {
        final int oldQueueSize = mQueue.size();
        final int oldMeteSize = mQueueMeta.size();
        ArrayList<QueueItem> newMeta = new ArrayList<>(mQueue.size());
        ListIterator<Uri> qi = mQueue.listIterator();
        ListIterator<QueueItem> qmi = mQueueMeta.listIterator();
        //if this was a change towards the end the start of the lists will be the same
        while (qi.hasNext() && qmi.hasNext()) {
            final Uri uri = qi.next();
            final QueueItem item = qmi.next();
            if (StringUtils.equals(uri.toString(), item.getDescription().getMediaId())) {
                newMeta.add(item);
            } else {
                //rewind;
                qi.previous();
                qmi.previous();
                break;
            }
        }
        while (qi.hasNext()) {
            final Uri uri = qi.next();
            QueueItem queueItem = null;
            //check if item exits in another position.
            for (QueueItem item2 : mQueueMeta) {
                if (uri.toString().equals(item2.getDescription().getMediaId())) {
                    queueItem = item2;
                    break;
                }
            }
            if (queueItem == null) {
                //new item
                queueItem = makeNewQueueItem(uri, descriptions);
            }
            if (queueItem == null) {
                final int idx = qi.previousIndex();
                //item was removed externally
                qi.remove();
                if (idx == mCurrentPos) {
                    //TODO i dont know if this is right
                    mCurrentPos = clamp(mCurrentPos - 1);
                }
            } else {
                newMeta.add(queueItem);
            }
        }
        mQueueMeta.clear();
        mQueueMeta.addAll(newMeta);
        if (mQueueMeta.size() * 2 < oldMeteSize) {
            mQueueMeta.trimToSize();//trim if much smaller than before
        }
    }

    //Exposed for testing
    /*package*/ QueueItem makeNewQueueItem(Uri uri, List<MediaDescription> descriptions) {
        for (MediaDescription desc : descriptions) {
            if (uri.toString().equals(desc.getMediaId())) {
                return new QueueItem(desc, mIdGenerator.incrementAndGet());
            }
        }
        return null;
    }

    private void updateDescriptions(final Action0 callbackaction) {
        if (mLookupSub != null) {
            mLookupSub.unsubscribe();
        }
        if (mQueue.isEmpty()) {
            mQueueMeta.clear();
            callbackaction.call();
            return;
        }
        List<Uri> urisToFetch = new ArrayList<>();
        if (!mQueueMeta.isEmpty()) {
            for (Uri uri : mQueue) {
                boolean found = false;
                for (QueueItem item : mQueueMeta) {
                    if (uri.toString().equals(item.getDescription().getMediaId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    urisToFetch.add(uri);
                }
            }
        } else {
            urisToFetch.addAll(mQueue);
        }
        if (urisToFetch.isEmpty()) {
            updateQueueMeta(Collections.<MediaDescription>emptyList());
            callbackaction.call();
        } else {
            mLookupSub = mIndexClient.getDescriptions(urisToFetch)
                    .observeOn(mService.getScheduler())
                    .subscribe(new Action1<List<MediaDescription>>() {
                        @Override
                        public void call(List<MediaDescription> mediaDescriptions) {
                            updateQueueMeta(mediaDescriptions);
                            callbackaction.call();
                            mLookupSub = null;
                        }
                    });
        }
    }

    void dumpLists() {
        if (mQueue.size() != mQueueMeta.size()) {
            Timber.e("Queues don't match");
        }
        ListIterator<Uri> qi = mQueue.listIterator();
        ListIterator<QueueItem> qmi = mQueueMeta.listIterator();
        int ii=0;
        while (qi.hasNext() && qmi.hasNext()) {
            Uri uri = qi.next();
            QueueItem queueItem = qmi.next();
            Timber.v("q %d -> %s\nm %d -> %s", ii++, uri, queueItem.getQueueId(),
                    queueItem.getDescription().getMediaId());
        }
    }

    private void notifyCurrentPosChanged() {
        updateDescriptions(new Action0() {
            @Override
            public void call() {
                if (mListener != null) {
                    mListener.onCurrentPosChanged();
                }
            }
        });
    }

    private void notifyQueueChanged() {
        updateDescriptions(new Action0() {
            @Override
            public void call() {
                if (mListener != null) {
                    mListener.onQueueChanged();
                }
            }
        });
    }

    private void notifyWentToNext() {
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
