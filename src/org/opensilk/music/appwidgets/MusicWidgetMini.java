package org.opensilk.music.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;

import org.opensilk.music.artwork.ArtworkProviderUtil;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 3/30/14.
 */
public class MusicWidgetMini extends MusicWidgetBase {

    @DebugLog
    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if (intent.getAction().equals(MusicWidgetService.QUERY_RESPONSE)) {
            mAlbumId = intent.getLongExtra("album_id", -1);
            mAlbumName = intent.getStringExtra("album");
            mArtistName = intent.getStringExtra("artist");
            mIsPlaying = intent.getBooleanExtra("playing", false);
            updateWidget(context, getClass());
        } else {
            Intent queryIntent = new Intent(MusicWidgetService.QUERY_MUSIC,
                    null, context, MusicWidgetService.class);
            context.startService(queryIntent);
        }
    }

    @Override
    public RemoteViews createView(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_widget_mini);
        ArtworkProviderUtil util = new ArtworkProviderUtil(context);
        Bitmap artwork = util.getArtworkThumbnail(mArtistName, mAlbumName, mAlbumId);
        ComponentName serviceName = new ComponentName(context, MusicPlaybackService.class);
        PendingIntent pendingIntent;

        // Album artwork
        views.setImageViewBitmap(R.id.widget_mini_album_art, artwork);

        // Pause / Play
        pendingIntent = buildPendingIntent(context, MusicPlaybackService.TOGGLEPAUSE_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_play, pendingIntent);
        views.setImageViewResource(R.id.widget_mini_play, mIsPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);

        // Next / Prev
        pendingIntent = buildPendingIntent(context, MusicPlaybackService.PREVIOUS_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_previous, pendingIntent);
        pendingIntent = buildPendingIntent(context, MusicPlaybackService.NEXT_ACTION, serviceName);
        views.setOnClickPendingIntent(R.id.widget_mini_next, pendingIntent);

        return views;
    }
}
