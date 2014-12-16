
package org.opensilk.music.ui2.loader;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;

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

    private Uri mUri;
    private String[] mProjection;
    private String mSelection;
    private String[] mSelectionArgs;

    public OrderPreservingCursor(final Context context, long[] ids) {
        super();
        mContext = context;
        mQuery = ids;
        mUri = Uris.EXTERNAL_MEDIASTORE_MEDIA;
        mProjection= Projections.LOCAL_SONG;
        mSelection = Selections.LOCAL_SONG + " AND ";
        mSelectionArgs = SelectionArgs.LOCAL_SONG;
        makeDelegateCursor();
    }

    public OrderPreservingCursor(Context context, long[] ids,
                                 Uri uri, String[] projection,
                                 String selection, String[] selectionArgs) {
        super();
        mContext = context;
        mQuery = ids;
        mUri = uri;
        mProjection = projection;
        mSelection = !TextUtils.isEmpty(selection) ? selection + " AND " : "";
        mSelectionArgs = selectionArgs;
        makeDelegateCursor();
    }

    @Override
    public int getCount() {
        return mSize;
    }

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

    @Override
    public String getString(final int column) {
        try {
            return mDelegateCursor.getString(column);
        } catch (final Exception ignored) {
            onChange(true);
            return "";
        }
    }

    @Override
    public short getShort(final int column) {
        return mDelegateCursor.getShort(column);
    }

    @Override
    public int getInt(final int column) {
        try {
            return mDelegateCursor.getInt(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public long getLong(final int column) {
        try {
            return mDelegateCursor.getLong(column);
        } catch (final Exception ignored) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public float getFloat(final int column) {
        return mDelegateCursor.getFloat(column);
    }

    @Override
    public double getDouble(final int column) {
        return mDelegateCursor.getDouble(column);
    }

    @Override
    public int getType(final int column) {
        return mDelegateCursor.getType(column);
    }

    @Override
    public boolean isNull(final int column) {
        return mDelegateCursor.isNull(column);
    }

    @Override
    public String[] getColumnNames() {
        return mProjection;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void deactivate() {
        if (mDelegateCursor != null) {
            mDelegateCursor.deactivate();
        }
    }

    @Override
    public boolean requery() {
        makeDelegateCursor();
        return true;
    }

    @Override
    public void close() {
        try {
            if (mDelegateCursor != null) {
                mDelegateCursor.close();
                mDelegateCursor = null;
            }
        } catch (final Exception close) {
        } finally {
            super.close();
        }
    }

    private void makeDelegateCursor() {
        resetDelegateCursor();

        mSize = mQuery.length;

        if (mSize == 0) {
            resetDelegateCursor();
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
            resetDelegateCursor();
            return;
        }

        mDelegateCursor = mContext.getContentResolver().query(
                mUri,
                mProjection,
                mSelection + selection.toString(),
                mSelectionArgs,
                BaseColumns._ID);

        if (mDelegateCursor == null) {
            resetDelegateCursor();
            return;
        }

        try {
            final int playlistSize = mDelegateCursor.getCount();
            mCursorIndexes = new long[playlistSize];
            if (mDelegateCursor.moveToFirst()) {
                final int columnIndex = mDelegateCursor.getColumnIndexOrThrow(BaseColumns._ID);
                for (int i = 0; i < playlistSize; i++) {
                    mCursorIndexes[i] = mDelegateCursor.getLong(columnIndex);
                    mDelegateCursor.moveToNext();
                }
            }
            mDelegateCursor.moveToFirst();
            mCurPos = -1;
        } catch (Exception e) {
            resetDelegateCursor();
        }

    }

    void resetDelegateCursor() {
        if (mDelegateCursor != null && !mDelegateCursor.isClosed()) {
            mDelegateCursor.close();
        }
        mDelegateCursor = null;
        mSize = 0;
    }

}
