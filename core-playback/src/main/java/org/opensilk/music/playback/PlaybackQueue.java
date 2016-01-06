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
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.service.PlaybackServiceProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackQueue {

    private final IndexClient mIndexClient;
    private final PlaybackServiceProxy mService;
    private final ArrayList<Uri> mQueue = new ArrayList<>();
    private final ArrayList<QueueItem> mQueueMeta = new ArrayList<>();
    private final TreeSet<Uri> mHistory = new TreeSet<>();
    private final Random mRandom = new Random();

    private int mCurrentPos = -1;
    private int mNextPos = -1;
    private int mPreviousPos = -1;
    private int mRepeatMode = PlaybackConstants.REPEAT_ALL;
    private int mShuffleMode = PlaybackConstants.SHUFFLE_NONE;
    private QueueChangeListener mListener;
    protected final AtomicLong mIdGenerator = new AtomicLong(1);
    private Subscription mLookupSub;
    private boolean mReady;

    @Inject
    public PlaybackQueue(
            IndexClient mIndexClient,
            PlaybackServiceProxy mService
    ) {
        this.mIndexClient = mIndexClient;
        this.mService = mService;
    }

    public boolean isReady() {
        return mReady;
    }

    @DebugLog
    public void load() {
        mReady = false;
        resetState();

        mIndexClient.startBatch();

        List<Uri> lastQueue = mIndexClient.getLastQueue();
        if (lastQueue != null && !lastQueue.isEmpty()) {
            mQueue.addAll(lastQueue);
        }
        int pos = mIndexClient.getLastQueuePosition();
        if (isInQueueBounds(pos)) {
            updateCurrentPos(pos);
        } else if (!mQueue.isEmpty()) {
            updateCurrentPos(0);
        }

        int rep = mIndexClient.getLastQueueRepeatMode();
        switch (rep) {
            case PlaybackConstants.REPEAT_NONE:
            case PlaybackConstants.REPEAT_ALL:
            case PlaybackConstants.REPEAT_CURRENT:
                mRepeatMode = rep;
                break;
            default:
                mRepeatMode = PlaybackConstants.REPEAT_ALL;
                break;
        }

        int shuf = mIndexClient.getLastQueueShuffleMode();
        switch (shuf) {
            case PlaybackConstants.SHUFFLE_NONE:
            case PlaybackConstants.SHUFFLE_NORMAL:
                mShuffleMode = shuf;
                break;
            default:
                mShuffleMode = PlaybackConstants.SHUFFLE_NONE;
                break;
        }

        mIndexClient.endBatch();

        if (isInQueueBounds(mCurrentPos)) {
            updateNextPos(mCurrentPos);
            updatePreviousPos(mCurrentPos, -1);
        }

        notifyCurrentPosChanged();

    }

    public void addNext(List<Uri> list) {
        if (mQueue.isEmpty() || !isInQueueBounds(mCurrentPos)) {
            resetState();
            mQueue.addAll(0, list);
            goToItem(0);
        } else {
            mQueue.addAll(mCurrentPos + 1, list);
            updateNextPos(mCurrentPos);
            notifyQueueChanged();
        }
    }

    public void addEnd(List<Uri> list) {
        if (mQueue.isEmpty() || !isInQueueBounds(mCurrentPos)) {
            resetState();
            mQueue.addAll(list);
            goToItem(0);
        } else {
            mQueue.addAll(list);
            updateNextPos(mCurrentPos);
            notifyQueueChanged();
        }
    }

    public void replace(List<Uri> list) {
        replace(list, 0);
    }

    public void replace(List<Uri> list, int startpos) {
        resetState();
        if (list == null || list.isEmpty()) {
            notifyCurrentPosChanged();
            return;
        }
        mQueue.addAll(list);
        mQueue.trimToSize();
        goToItem(startpos);
    }

    public void remove(List<Uri> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Uri current = null;
        if (isInQueueBounds(mCurrentPos)) {
            current = mQueue.get(mCurrentPos);
        }
        Uri next = null;
        if (isInQueueBounds(mNextPos)) {
            next = mQueue.get(mNextPos);
        }
        Uri prev = null;
        if (isInQueueBounds(mPreviousPos)) {
            prev = mQueue.get(mPreviousPos);
        }
        mQueue.removeAll(list);
        mHistory.removeAll(list);
        if (mQueue.isEmpty()) {
            resetState();
            notifyCurrentPosChanged();
            return;
        }
        int curIdx = -1;
        if (current != null) {
            curIdx = mQueue.indexOf(current);
        }
        int nextIdx = -1;
        if (next != null) {
            nextIdx = mQueue.indexOf(next);
        }
        int prevIdx = -1;
        if (prev != null) {
            prevIdx = mQueue.indexOf(prev);
        }
        if (isInQueueBounds(curIdx)) {
            //still have current update pointer
            updateCurrentPos(curIdx);
            updateNextPos(mCurrentPos);
            updatePreviousPos(mCurrentPos, prevIdx);
            notifyQueueChanged();
        } else {
            //Current was removed
            if (isInQueueBounds(nextIdx)) {
                //we have next make it current
                updateCurrentPos(nextIdx);
            } else {
                //re-clamp old current as current
                updateCurrentPos(mCurrentPos);
            }
            updateNextPos(mCurrentPos);
            updatePreviousPos(mCurrentPos, prevIdx);
            notifyCurrentPosChanged();
        }
    }

    public void remove(Uri uri) {
        if (!mQueue.contains(uri)) {
            return;
        }
        remove(Collections.singletonList(uri));
    }

    public void remove(int pos) {
        if (!isInQueueBounds(pos)) {
            return;
        }
        remove(mQueue.get(pos));
    }

    public void moveItem(Uri uri, int to) {
        if (uri == null || mQueue.isEmpty() || !mQueue.contains(uri)) {
            return;
        }
        Uri current = null;
        if (isInQueueBounds(mCurrentPos)) {
            current = mQueue.get(mCurrentPos);
        }
        Uri next = null;
        if (isInQueueBounds(mNextPos)) {
            next = mQueue.get(mNextPos);
        }
        Uri prev = null;
        if (isInQueueBounds(mPreviousPos)) {
            prev = mQueue.get(mPreviousPos);
        }
        mQueue.remove(uri);
        mQueue.add(clamp(to), uri);
        int curIdx = -1;
        if (current != null) {
            curIdx = mQueue.indexOf(current);
        }
        int nextIdx = -1;
        if (next != null) {
            nextIdx = mQueue.indexOf(next);
        }
        int prevIdx = -1;
        if (prev != null) {
            prevIdx = mQueue.indexOf(prev);
        }
        if (mCurrentPos != curIdx) {
            updateCurrentPos(curIdx);
        }
        if (isShuffleOn() && isInQueueBounds(nextIdx)) {
            mNextPos = nextIdx;
        } else {
            updateNextPos(mCurrentPos);
        }
        updatePreviousPos(mCurrentPos, prevIdx);
        if (current == null) {
            notifyCurrentPosChanged();
        } else {
            notifyQueueChanged();
        }
    }

    public void moveItem(int from, int to) {
        if (from == to || !isInQueueBounds(from)) {
            return;
        }
        moveItem(mQueue.get(from), to);
    }

    public void clear() {
        resetState();
        notifyCurrentPosChanged();
    }

    public int getPosOfId(long id) {
        int pos = -1;
        for (int ii=0; ii<mQueueMeta.size(); ii++) {
            if (mQueueMeta.get(ii).getQueueId() == id) {
                pos = ii;
                break;
            }
        }
        return pos;
    }

    public void goToItem(int pos) {
        if (mCurrentPos == pos) {
            return;
        }
        int oldCurrent = mCurrentPos;
        updateCurrentPos(pos);
        updateNextPos(mCurrentPos);
        updatePreviousPos(mCurrentPos, oldCurrent);
        notifyCurrentPosChanged();
    }

    public void moveToNext() {
        if (isInQueueBounds(mNextPos)) {
            int oldCurrent = mCurrentPos;
            updateCurrentPos(mNextPos);
            updateNextPos(mCurrentPos);
            updatePreviousPos(mCurrentPos, oldCurrent);
        }
        notifyWentToNext();
    }

    public int getNextPos() {
        return mNextPos;
    }

    public boolean hasNext() {
        return isInQueueBounds(mNextPos);
    }

    public int getPreviousPos() {
        return mPreviousPos;
    }

    public boolean hasPrevious() {
        return isInQueueBounds(mPreviousPos);
    }

    public int getCurrentPos() {
        return mCurrentPos;
    }

    public boolean hasCurrent() {
        return isInQueueBounds(mCurrentPos);
    }

    public List<Uri> get() {
        return new ArrayList<>(mQueue);
    }

    public void shuffle() {
        Uri current = null;
        if (isInQueueBounds(mCurrentPos)) {
            current = mQueue.get(mCurrentPos);
        }
        Uri next = null;
        if (isInQueueBounds(mNextPos)) {
            next = mQueue.get(mNextPos);
        }
        Uri prev = null;
        if (isInQueueBounds(mPreviousPos)) {
            prev = mQueue.get(mPreviousPos);
        }
        randomizeQueue();
        int curIdx = -1;
        if (current != null) {
            curIdx = mQueue.indexOf(current);
        }
        int nextIdx = -1;
        if (next != null) {
            nextIdx = mQueue.indexOf(next);
        }
        int prevIdx = -1;
        if (prev != null) {
            prevIdx = mQueue.indexOf(prev);
        }
        if (isInQueueBounds(curIdx)) {
            updateCurrentPos(curIdx);
        } else {
            updateCurrentPos(0);
        }
        if (isShuffleOn() && isInQueueBounds(nextIdx)) {
            mNextPos = nextIdx;
        } else {
            updateNextPos(mCurrentPos);
        }
        updatePreviousPos(mCurrentPos, prevIdx);
        if (current == null) {
            notifyCurrentPosChanged();
        } else {
            notifyQueueChanged();
        }
    }

    private void randomizeQueue() {
        Collections.shuffle(mQueue);
    }

    public boolean notEmpty() {
        return !mQueue.isEmpty();
    }

    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    public @Nullable Uri getCurrentUri() {
        return isInQueueBounds(mCurrentPos) ? mQueue.get(mCurrentPos) : null;
    }

    public @Nullable Uri getNextUri() {
        return isInQueueBounds(mNextPos) ? mQueue.get(mNextPos) : null;
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    private boolean isRepeatCurrent() {
        return mRepeatMode == PlaybackConstants.REPEAT_CURRENT;
    }

    private boolean isRepeatAll() {
        return mRepeatMode == PlaybackConstants.REPEAT_ALL;
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
        updateNextPos(mCurrentPos);
        updatePreviousPos(mCurrentPos, mPreviousPos);
        notifyQueueChanged();
    }

    //exposed for testing
    void setRepeatMode(int newMode) {
        if (mRepeatMode == newMode) {
            return;
        }
        mRepeatMode = newMode;
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
        updateNextPos(mCurrentPos);
        updatePreviousPos(mCurrentPos, mPreviousPos);
        notifyQueueChanged();
    }

    //exposed for testing
    void setShuffleMode(int newMode) {
        if (mShuffleMode == newMode) {
            return;
        }
        mShuffleMode = newMode;
    }

    private int clamp(int pos) {
        return (pos < 0) ? 0 : (pos >= mQueue.size()) ? (mQueue.size() - 1) : pos;
    }

    private boolean isInQueueBounds(int pos) {
        return pos >= 0 && pos < mQueue.size();
    }

    private void updateCurrentPos(int pos) {
        mCurrentPos = clamp(pos);
        if (isInQueueBounds(mCurrentPos)) {
            mHistory.add(mQueue.get(mCurrentPos));
        }
    }

    private void updateNextPos(int current) {
        if (isRepeatCurrent()) {
            mNextPos = current;
        } else if (isShuffleOn()) {
            int next = mRandom.nextInt(mQueue.size());
            //TODO handle same uri in multiple positions
            while (mHistory.contains(mQueue.get(next)) && next > 0) {
                next--;
            }
            if (next == 0) {
                //find something that hasn't been played
                for (int ii = mQueue.size() - 1; ii >= 0; ii--) {
                    if (!mHistory.contains(mQueue.get(ii))) {
                        next = ii;
                        break;
                    }
                }
            }
            if (next == 0) {
                //Queue has been exhausted
                mHistory.clear();
                mNextPos = isRepeatAll() ? 0 : -1;
            } else {
                mNextPos = next;
            }
        } else if (isInQueueBounds(current + 1)) {
            mNextPos = current + 1;
        } else {
            mNextPos = isRepeatAll() ? 0 : -1;
        }
    }

    private void updatePreviousPos(int current, int oldCurrent) {
        if (isShuffleOn() && !isRepeatCurrent()) {
            if (isInQueueBounds(oldCurrent)) {
                mPreviousPos = oldCurrent;
            } else {
                mPreviousPos = -1;
            }
        } else {
            if (isRepeatCurrent()) {
                mPreviousPos = -1;
            } else {
                mPreviousPos = isInQueueBounds(current - 1) ? (current - 1)
                        : (isRepeatAll() ? (mQueue.size() - 1) : -1);
            }
        }
    }

    private void resetState() {
        mQueue.clear();
        mHistory.clear();
        resetPositions();
    }

    private void resetPositions() {
        mCurrentPos = -1;
        mNextPos = -1;
        mPreviousPos = -1;
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
    private void updateQueueMeta(List<MediaDescriptionCompat> descriptions) {
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
    /*package*/ QueueItem makeNewQueueItem(Uri uri, List<MediaDescriptionCompat> descriptions) {
        for (MediaDescriptionCompat desc : descriptions) {
            if (uri.toString().equals(desc.getMediaId())) {
                return new QueueItem(desc, mIdGenerator.incrementAndGet());
            }
        }
        return null;
    }

    private void updateDescriptions(final Action0 callbackaction) {
        mReady = true;
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
            updateQueueMeta(Collections.<MediaDescriptionCompat>emptyList());
            callbackaction.call();
        } else {
            mReady = false;
            mLookupSub = mIndexClient.getDescriptions(urisToFetch)
                    .first()
                    .observeOn(mService.getScheduler())
                    .subscribe(new Subscriber<List<MediaDescriptionCompat>>() {
                        @Override public void onCompleted() {
                            mLookupSub = null;
                        }
                        @Override public void onError(Throwable e) {
                            Timber.e(e, "What to do?");
                            mLookupSub = null;
                        }
                        @Override public void onNext(List<MediaDescriptionCompat> mediaDescriptions) {
                            updateQueueMeta(mediaDescriptions);
                            mReady = true;
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

    public Snapshot snapshot() {
        return new Snapshot(mQueue, mCurrentPos, mRepeatMode, mShuffleMode);
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

}
