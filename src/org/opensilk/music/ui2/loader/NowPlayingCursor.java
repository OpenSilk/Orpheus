
package org.opensilk.music.ui2.loader;

import android.content.Context;

import com.andrew.apollo.provider.MusicProvider;

import org.opensilk.music.util.Projections;

public class NowPlayingCursor extends OrderPreservingCursor {

    public NowPlayingCursor(Context context, long[] ids) {
        super(context, ids, MusicProvider.RECENTS_URI, Projections.RECENT_SONGS, null, null);
    }

}
