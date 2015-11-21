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

package org.opensilk.music.appwidgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import org.opensilk.music.R;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 4/3/14.
 */
public class MusicWidgetSmall extends MusicWidgetBase {

    @Override
    @DebugLog
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_widget_small);

        setupArtwork(context, views);
        setupArtworkIntent(context, views);

        setupTrackTitle(context, views);
        setupArtistName(context, views);

        setupPlayBtn(context, views);
        setupNextBtnIntent(context, views);

        postUpdate(appWidgetManager, appWidgetIds, views);

    }

}
