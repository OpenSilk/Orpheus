package org.opensilk.music.appwidgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import org.opensilk.music.R;

/**
 * Created by drew on 12/9/15.
 */
public class MusicWidgetLarge extends MusicWidgetBase {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_widget_large);

        setupArtwork(context, views);
        setupArtworkIntent(context, views);

        setupTrackTitle(context, views);
        setupArtistPlusAlbumName(context, views);

        setupPlayBtn(context, views);
        setupPreviousBtnIntent(context, views);
        setupNextBtnIntent(context, views);
        //TODO setup repeat/shuffle icons
        setupShuffleBtnIntent(context, views);
        setupRepeatBtnIntent(context, views);

        postUpdate(appWidgetManager, appWidgetIds, views);
    }
}
