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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

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

    public static AppCompatActivity findActivity(Context context) {
        if (context instanceof Activity) {
            return (AppCompatActivity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper)context).getBaseContext());
        } else {
            throw new IllegalArgumentException("Unable to find activty in context");
        }
    }

    /**
     * @param context The {@link Context} to use
     * @param id The song ID.
     */
    public static boolean setRingtone(final Context context, final long id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, "1");
            values.put(MediaStore.Audio.AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (final UnsupportedOperationException ingored) {
            Toast.makeText(context, R.string.err_generic, Toast.LENGTH_SHORT).show();
            return false;
        }

        final String[] projection = new String[] {
                BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE
        };

        final String selection = BaseColumns._ID + "=" + id;
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                selection, null, null);
        try {
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                Toast.makeText(context, context.getString(R.string.set_as_ringtone,
                        cursor.getString(2)), Toast.LENGTH_SHORT).show();
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        Toast.makeText(context, R.string.err_generic, Toast.LENGTH_SHORT).show();
        return false;
    }

}
