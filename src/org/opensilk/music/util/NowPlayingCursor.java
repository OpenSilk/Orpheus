
package org.opensilk.music.util;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.RemoteException;

import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicStore;

import org.opensilk.music.ui2.main.MusicServiceConnection;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.Arrays;

/**
 * A custom {@link android.database.Cursor} used to return the queue and allow for easy dragging
 * and dropping of the items in it.
 */
public class NowPlayingCursor extends AbstractCursor {

    private final Context mContext;

    private final MusicServiceConnection mServiceConnection;

    private long[] mNowPlaying;

    private long[] mCursorIndexes;

    private int mSize;

    private int mCurPos;

    private Cursor mQueueCursor;

    /**
     * Constructor of <code>NowPlayingCursor</code>
     *
     * @param context The {@link Context} to use
     */
    public NowPlayingCursor(Context context,
                            MusicServiceConnection musicServiceConnection) {
        mContext = context;
        mServiceConnection = musicServiceConnection;
        makeNowPlayingCursor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMove(final int oldPosition, final int newPosition) {
        if (oldPosition == newPosition) {
            return true;
        }

        if (mNowPlaying == null || mCursorIndexes == null || newPosition >= mNowPlaying.length) {
            return false;
        }

        final long id = mNowPlaying[newPosition];
        final int cursorIndex = Arrays.binarySearch(mCursorIndexes, id);
        mQueueCursor.moveToPosition(cursorIndex);
        mCurPos = newPosition;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final int column) {
        try {
            return mQueueCursor.getString(column);
        } catch (final Exception ignored) {
            onChange(true);
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort(final int column) {
        return mQueueCursor.getShort(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final int column) {
        try {
            return mQueueCursor.getInt(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final int column) {
        try {
            return mQueueCursor.getLong(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(final int column) {
        return mQueueCursor.getFloat(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final int column) {
        return mQueueCursor.getDouble(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType(final int column) {
        return mQueueCursor.getType(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNull(final int column) {
        return mQueueCursor.isNull(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getColumnNames() {
        return Projections.RECENT_SONGS;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public void deactivate() {
        if (mQueueCursor != null) {
            mQueueCursor.deactivate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requery() {
        makeNowPlayingCursor();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            if (mQueueCursor != null) {
                mQueueCursor.close();
                mQueueCursor = null;
            }
        } catch (final Exception close) {
        }
        super.close();
    };

    /**
     * Actually makes the queue
     */
    private void makeNowPlayingCursor() {
        if (mQueueCursor != null && !mQueueCursor.isClosed()) {
            mQueueCursor.close();
        }
        mQueueCursor = null;
        mNowPlaying = mServiceConnection.getQueue().toBlocking().first();
        mSize = mNowPlaying.length;
        if (mSize == 0) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        selection.append(MusicStore.Cols._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mSize - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        mQueueCursor = mContext.getContentResolver().query(
                MusicProvider.RECENTS_URI,
                Projections.RECENT_SONGS,
                selection.toString(),
                null,
                MusicStore.Cols._ID);

        if (mQueueCursor == null) {
            mSize = 0;
            return;
        }

        final int playlistSize = mQueueCursor.getCount();
        mCursorIndexes = new long[playlistSize];
        mQueueCursor.moveToFirst();
        final int columnIndex = mQueueCursor.getColumnIndexOrThrow(MusicStore.Cols._ID);
        for (int i = 0; i < playlistSize; i++) {
            mCursorIndexes[i] = mQueueCursor.getLong(columnIndex);
            mQueueCursor.moveToNext();
        }
        mQueueCursor.moveToFirst();
        mCurPos = -1;

    }

}
