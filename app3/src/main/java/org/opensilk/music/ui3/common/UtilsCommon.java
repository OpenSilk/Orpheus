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
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import org.opensilk.music.R;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.sort.TrackSortOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import timber.log.Timber;

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
            ImageView artwork,
            ImageView artwork2,
            ImageView artwork3,
            ImageView artwork4,
            List<ArtInfo> artInfos
    ) {
        final int num = artInfos.size();
        if (artwork != null) {
            if (num >= 1) {
                requestor.newRequest(artInfos.get(0), artwork, null);
            } else {
                artwork.setImageResource(R.drawable.default_artwork);
            }
        }
        if (artwork2 != null) {
            if (num >= 2) {
                requestor.newRequest(artInfos.get(1), artwork2, null);
            } else {
                artwork2.setImageResource(R.drawable.default_artwork);
            }
        }
        if (artwork3 != null) {
            if (num >= 3) {
                requestor.newRequest(artInfos.get(2), artwork3, null);
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                requestor.newRequest(artInfos.get(1), artwork3, null);
            } else {
                artwork3.setImageResource(R.drawable.default_artwork);
            }
        }
        if (artwork4 != null) {
            if (num >= 4) {
                requestor.newRequest(artInfos.get(3), artwork4, null);
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                requestor.newRequest(artInfos.get(0), artwork4, null);
            } else {
                artwork4.setImageResource(R.drawable.default_artwork);
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

    public static List<Uri> filterTracks(List<Model> adapterItems) {
        if (adapterItems == null || adapterItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Uri> toPlay = new ArrayList<>(adapterItems.size());
        for (Model b : adapterItems) {
            if (b instanceof Track) {
                toPlay.add(((Track) b).getUri());
            }
        }
        return toPlay;
    }

    public static void addTracksToQueue(Context context, List<Uri> trackUris, final Action1<List<Uri>> addFunc) {
        List<Observable<List<Track>>> loaders = new ArrayList<>(trackUris.size());
        for (Uri uri : trackUris) {
            loaders.add(TypedBundleableLoader.<Track>create(context)
                    .setUri(uri).setSortOrder(TrackSortOrder.ALBUM)
                    .createObservable().retry(1));
        }
        Observable.concatEager(loaders)
                .collect(new Func0<List<Uri>>() {
                    @Override
                    public List<Uri> call() {
                        return new ArrayList<Uri>();
                    }
                }, new Action2<List<Uri>, List<Track>>() {
                    @Override
                    public void call(List<Uri> uris, List<Track> tracks) {
                        for (Track track : tracks) {
                            uris.add(track.getUri());
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addFunc, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.w(throwable, "addTracksToQueue");
                    }
                });
    }
}
