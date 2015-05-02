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

package org.opensilk.music.ui3.common;

import android.content.Context;

import org.opensilk.music.R;

/**
 * Created by drew on 5/2/15.
 */
public class Utils {
    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     *         albums, songs, genres, and playlists.
     */
    public static String makeLabel(final Context context, final int pluralInt,
                                   final int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    public static String makeTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs -= hours * 3600;
        mins = secs / 60;
        secs -= mins * 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }
}
