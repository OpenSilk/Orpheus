package org.opensilk.music.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.andrew.apollo.MusicPlaybackService;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 3/30/14.
 */
public abstract class MusicWidgetBase extends AppWidgetProvider {

    protected String mAlbumName = "";
    protected String mArtistName = "";
    protected String mTrackName = "";
    protected int mShuffleMode = -1;
    protected int mRepeateMode = -1;
    protected long mAlbumId = -1;
    protected boolean mIsPlaying = false;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // Call to populate our widgets album art with the previously played song
        context.startService(new Intent(MusicWidgetService.QUERY_MUSIC,
                null, context, MusicWidgetService.class));
    }

    protected PendingIntent buildPendingIntent(Context context, String action,
                                               ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, false);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    protected void updateWidget(Context context, Class cls) {
        ComponentName cn = new ComponentName(context, cls);
        AppWidgetManager.getInstance(context).updateAppWidget(cn, createView(context));
    }

    public abstract RemoteViews createView(Context context);

}
