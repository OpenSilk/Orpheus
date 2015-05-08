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
import android.net.Uri;

import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;

import java.util.List;

import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 5/2/15.
 */
public class UtilsCommon {
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

    public static ArtInfo makeBestfitArtInfo(String artist, String altArtist, String album, Uri uri) {
        return UtilsArt.makeBestfitArtInfo(artist, altArtist, album, uri);
    }

    public static void loadMultiArtwork(
            ArtworkRequestManager requestor,
            CompositeSubscription cs,
            AnimatedImageView artwork,
            AnimatedImageView artwork2,
            AnimatedImageView artwork3,
            AnimatedImageView artwork4,
            List<ArtInfo> artInfos,
            ArtworkType artworkType
    ) {
        final int num = artInfos.size();
        if (artwork != null) {
            if (num >= 1) {
                cs.add(requestor.newRequest(artwork, null, artInfos.get(0), artworkType));
            } else {
                artwork.setDefaultImage(R.drawable.default_artwork);
            }
        }
        if (artwork2 != null) {
            if (num >= 2) {
                cs.add(requestor.newRequest(artwork2, null, artInfos.get(1), artworkType));
            } else {
                // never get here
                artwork2.setDefaultImage(R.drawable.default_artwork);
            }
        }
        if (artwork3 != null) {
            if (num >= 3) {
                cs.add(requestor.newRequest(artwork3, null, artInfos.get(2), artworkType));
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                cs.add(requestor.newRequest(artwork3, null, artInfos.get(1), artworkType));
            } else {
                // never get here
                artwork3.setDefaultImage(R.drawable.default_artwork);
            }
        }
        if (artwork4 != null) {
            if (num >= 4) {
                cs.add(requestor.newRequest(artwork4, null, artInfos.get(3), artworkType));
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                cs.add(requestor.newRequest(artwork4, null, artInfos.get(0), artworkType));
            } else {
                //never get here
                artwork4.setDefaultImage(R.drawable.default_artwork);
            }
        }
    }

}
