
package com.andrew.apollo;

import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

public final class Shuffler {

    private static final int MAX_HISTORY_SIZE = MusicPlaybackService.MAX_HISTORY_SIZE;

    private final LinkedList<Integer> mHistoryOfNumbers;

    private final TreeSet<Integer> mPreviousNumbers;

    private final Random mRandom = new Random();

    private int mPrevious;

    /**
     * Constructor of <code>Shuffler</code>
     */
    public Shuffler() {
        super();
        mHistoryOfNumbers = new LinkedList<>();
        mPreviousNumbers = new TreeSet<>();
    }

    /**
     * @param interval The length the queue
     * @return The position of the next track to play
     */
    public int nextInt(final int interval) {
        int next;
        do {
            next = mRandom.nextInt(interval);
        } while (next == mPrevious && interval > 1
                && !mPreviousNumbers.contains(Integer.valueOf(next)));
        mPrevious = next;
        mHistoryOfNumbers.add(mPrevious);
        mPreviousNumbers.add(mPrevious);
        cleanUpHistory();
        return next;
    }

    /**
     * Removes old tracks and cleans up the history preparing for new tracks
     * to be added to the mapping
     */
    private void cleanUpHistory() {
        if (!mHistoryOfNumbers.isEmpty() && mHistoryOfNumbers.size() >= MAX_HISTORY_SIZE) {
            for (int i = 0; i < Math.max(1, MAX_HISTORY_SIZE / 2); i++) {
                mPreviousNumbers.remove(mHistoryOfNumbers.removeFirst());
            }
        }
    }
};
