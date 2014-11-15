
package org.opensilk.music.ui2.loader;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

import java.util.Arrays;

/**
 * A custom {@link Cursor} stolen from NowPlayingCursor
 * to allow quering mediastore for audio ids and have
 * the cursor iterate in the original order of the ids given
 */
public class OrderPreservingCursor extends AbstractCursor {

    private final Context mContext;

    private long[] mQuery;

    private long[] mCursorIndexes;

    private int mSize;

    private int mCurPos;

    private Cursor mDelegateCursor;

    /**
     * Constructor of <code>NowPlayingCursor</code>
     *
     * @param context The {@link Context} to use
     */
    public OrderPreservingCursor(final Context context, long[] ids) {
        super();
        mContext = context;
        mQuery = ids;
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

        if (mQuery == null || mCursorIndexes == null || newPosition >= mQuery.length) {
            return false;
        }

        final long id = mQuery[newPosition];
        final int cursorIndex = Arrays.binarySearch(mCursorIndexes, id);
        mDelegateCursor.moveToPosition(cursorIndex);
        mCurPos = newPosition;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final int column) {
        try {
            return mDelegateCursor.getString(column);
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
        return mDelegateCursor.getShort(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final int column) {
        try {
            return mDelegateCursor.getInt(column);
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
            return mDelegateCursor.getLong(column);
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
        return mDelegateCursor.getFloat(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final int column) {
        return mDelegateCursor.getDouble(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType(final int column) {
        return mDelegateCursor.getType(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNull(final int column) {
        return mDelegateCursor.isNull(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getColumnNames() {
        return Projections.LOCAL_SONG;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public void deactivate() {
        if (mDelegateCursor != null) {
            mDelegateCursor.deactivate();
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
            if (mDelegateCursor != null) {
                mDelegateCursor.close();
                mDelegateCursor = null;
            }
        } catch (final Exception close) {
        }
        super.close();
    }

    /**
     * Actually makes the queue
     */
    private void makeNowPlayingCursor() {
        mDelegateCursor = null;
        mSize = mQuery.length;
        if (mSize == 0) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            selection.append(mQuery[i]);
            if (i < mSize - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        if (mContext == null) {
            return;
        }

        mDelegateCursor = mContext.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Projections.LOCAL_SONG,
                Selections.LOCAL_SONG + " AND " + selection.toString(),
                SelectionArgs.LOCAL_SONG,
                BaseColumns._ID);

        if (mDelegateCursor == null) {
            mSize = 0;
            return;
        }

        final int playlistSize = mDelegateCursor.getCount();
        mCursorIndexes = new long[playlistSize];
        mDelegateCursor.moveToFirst();
        final int columnIndex = mDelegateCursor.getColumnIndexOrThrow(BaseColumns._ID);
        for (int i = 0; i < playlistSize; i++) {
            mCursorIndexes[i] = mDelegateCursor.getLong(columnIndex);
            mDelegateCursor.moveToNext();
        }
        mDelegateCursor.moveToFirst();
        mCurPos = -1;
    }

}
