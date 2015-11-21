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

package org.opensilk.music.playback.appwidget;

import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.music.playback.PlaybackConstants;

/**
 * Created by drew on 11/20/15.
 */
public class AppWidgetService {
    public static String SERVICE_NAME = AppWidgetService.class.getName();

    @SuppressWarnings("ResourceType")
    public static AppWidgetService getService(Context context) {
        return (AppWidgetService) context.getSystemService(SERVICE_NAME);
    }

    private MediaMetadataCompat meta;
    private PlaybackStateCompat playbackState;

    public void setMeta(MediaMetadataCompat meta) {
        this.meta = meta;
    }

    public MediaMetadataCompat getMeta() {
        return meta;
    }

    public void setPlaybackState(PlaybackStateCompat playbackState) {
        this.playbackState = playbackState;
    }

    public PlaybackStateCompat getPlaybackState() {
        return playbackState;
    }

    public void notify(Context context) {
        Intent intent = new Intent(PlaybackConstants.WIDGET_UPDATE_ACTION)
                .setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

}
