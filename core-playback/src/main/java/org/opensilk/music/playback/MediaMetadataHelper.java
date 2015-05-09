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

package org.opensilk.music.playback;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.service.ArtworkProviderHelper;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Track;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.*;

/**
 * Created by drew on 5/8/15.
 */
public class MediaMetadataHelper {

    final ArtworkProviderHelper providerHelper;
    final Scheduler scheduler = Schedulers.computation();

    final CurrentInfo currentInfo = new CurrentInfo();
    Subscription currentSubcription;
    MediaSession mediaSession;
    Scheduler oScheduler;

    static class CurrentInfo {
        Track track;
        ArtInfo artInfo;
        Bitmap bitmap;
        boolean hasAnyNull() {
            return track == null || artInfo == null || bitmap == null;
        }
    }

    @Inject
    public MediaMetadataHelper(ArtworkProviderHelper providerHelper) {
        this.providerHelper = providerHelper;
    }

    public void setMediaSession(MediaSession mediaSession, Handler handler) {
        this.mediaSession = mediaSession;
        this.oScheduler = AndroidSchedulers.handlerThread(handler);
    }

    public void updateMeta(Track track) {
        if (mediaSession == null || oScheduler == null) {
            Timber.e("Must setMediaSession");
            return;
        }

        if (track.equals(currentInfo.track)) {
            return;
        }

        currentInfo.track = track;

        final ArtInfo artInfo = UtilsArt.makeBestfitArtInfo(track.albumArtistName,
                track.artistName, track.albumName, track.artworkUri);

        if (artInfo.equals(currentInfo.artInfo)) {
            if (currentInfo.bitmap != null) {
                setMeta();
            } // else wait for artwork
            return;
        }

        currentInfo.artInfo = artInfo;

        if (currentSubcription != null) {
            currentSubcription.unsubscribe();
        }
        currentSubcription = providerHelper.getArtwork(artInfo, ArtworkType.THUMBNAIL)
                .subscribeOn(scheduler)
                .observeOn(oScheduler)
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        currentInfo.bitmap = bitmap;
                        setMeta();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "getArtwork");
                        currentSubcription = null;
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        currentSubcription = null;
                    }
                });
    }

    void setMeta() {
        if (currentInfo.hasAnyNull() || mediaSession == null) {
            return;
        }
        final Track t = currentInfo.track;
        final Bitmap b = currentInfo.bitmap;
        final long duration = mediaSession.getController().getPlaybackState().getBufferedPosition();
        MediaMetadataCompat m = new MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_TITLE, t.name)
                .putString(METADATA_KEY_DISPLAY_TITLE, t.name)
                .putString(METADATA_KEY_ARTIST, t.artistName)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, t.artistName)
                //.putString(METADATA_KEY_DISPLAY_DESCRIPTION, TODO)
                .putString(METADATA_KEY_ALBUM_ARTIST,
                        StringUtils.isEmpty(t.albumArtistName) ? t.artistName : t.albumArtistName)
                .putString(METADATA_KEY_ALBUM, t.albumName)
                .putLong(METADATA_KEY_DURATION, duration)
                .putBitmap(METADATA_KEY_ALBUM_ART, b)
                //.putString(METADATA_KEY_ALBUM_ART_URI, TODO)
                //.putString(METADATA_KEY_MEDIA_ID, TODO)
                .build();
        mediaSession.setMetadata((MediaMetadata)m.getMediaMetadata());
    }
}
