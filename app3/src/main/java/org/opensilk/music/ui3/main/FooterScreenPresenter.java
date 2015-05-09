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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static android.support.v4.media.MediaMetadataCompat.*;
import static android.support.v4.media.session.PlaybackStateCompat.*;


/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class FooterScreenPresenter extends ViewPresenter<FooterScreenView> implements PausesAndResumes {

    final Context appContext;
    final PlaybackController playbackController;
    final ArtworkRequestManager artworkReqestor;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final AppPreferences settings;

    final CurrentInfo currentInfo = new CurrentInfo();

    static class CurrentInfo {
        String trackName;
        String artistName;
        Bitmap albumArt;
        long position;
        long duration;
        boolean positionSynced;
    }

    CompositeSubscription broadcastSubscriptions;

    @Inject
    public FooterScreenPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            ArtworkRequestManager artworkReqestor,
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
        this.artworkReqestor = artworkReqestor;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope()");
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad()");
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            subscribeBroadcasts();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        Timber.v("onSave()");
        super.onSave(outState);
        if (!hasView() && pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
            unsubscribeBroadcasts();
        }
    }

    @Override
    public void onResume() {
        Timber.v("onResume()");
        if (hasView()) {
            subscribeBroadcasts();
        }
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        unsubscribeBroadcasts();
    }

    void setTrackName(String s) {
        if (!StringUtils.equals(currentInfo.trackName, s)) {
            currentInfo.trackName = s;
            if (hasView()) {
                getView().trackTitle.setText(s);
            }
        }
    }

    void setArtistName(String s) {
        if (!StringUtils.equals(currentInfo.artistName, s)) {
            currentInfo.artistName = s;
            if (hasView()) {
                getView().artistName.setText(s);
            }
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().progressBar.setIndeterminate(progress == -1);
            getView().progressBar.setProgress(progress);
        }
    }

    void updateArtwork(Bitmap bitmap) {
        if (bitmap != null) {
            if (!bitmap.sameAs(currentInfo.albumArt)) {
                currentInfo.albumArt = bitmap;
                if (hasView()) {
                    getView().artworkThumbnail.setImageBitmap(bitmap, true);
                }
            }
        } else if (hasView()) {
            getView().artworkThumbnail.setDefaultImage(R.drawable.default_artwork);
        }
    }

    void onClick(Context context) {
        handleClick(settings.getString(AppPreferences.FOOTER_CLICK, AppPreferences.ACTION_OPEN_QUEUE), context);
    }

    boolean onLongClick(Context context) {
        return handleClick(settings.getString(AppPreferences.FOOTER_LONG_CLICK, AppPreferences.ACTION_NONE), context);
    }

    void onThumbClick(Context context) {
        handleClick(settings.getString(AppPreferences.FOOTER_THUMB_CLICK, AppPreferences.ACTION_OPEN_NOW_PLAYING), context);
    }

    boolean onThumbLongClick(Context context) {
        return handleClick(settings.getString(AppPreferences.FOOTER_THUMB_LONG_CLICK, AppPreferences.ACTION_NONE), context);
    }

    @DebugLog
    boolean handleClick(String action, Context context) {
        switch (action) {
            case AppPreferences.ACTION_OPEN_QUEUE:
                //QueueScreen.toggleQueue(context);
                return true;
            case AppPreferences.ACTION_OPEN_NOW_PLAYING:
                //NowPlayingScreen.toggleNowPlaying(context);
                return true;
            case AppPreferences.ACTION_NONE:
            default:
                return false;
        }
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            return;
        }
        Subscription s = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        final int state = playbackState.getState();
                        if (state == STATE_BUFFERING || state == STATE_CONNECTING) {
                            setProgress(-1);
                        } else {
                            long position = playbackState.getPosition();
                            long duration = playbackState.getBufferedPosition();
                            if (position < 0 || duration <= 0) {
                                setProgress(1000);
                            } else {
                                setProgress((int) (1000 * position / duration));
                            }
                            Timber.v("Position discrepancy = %d", currentInfo.position - position);
                            currentInfo.position = position;
                            currentInfo.duration = duration;
                            currentInfo.positionSynced = true;
                        }

                    }
                }
        );
        Subscription s2 = playbackController.subscribeMetaChanges(
                new Action1<MediaMetadataCompat>() {
                    @Override
                    public void call(MediaMetadataCompat mediaMetadata) {
                        setTrackName(mediaMetadata.getString(METADATA_KEY_TITLE));
                        setArtistName(mediaMetadata.getString(METADATA_KEY_ARTIST));
                        updateArtwork(mediaMetadata.getBitmap(METADATA_KEY_ALBUM_ART));
                    }
                }
        );
        final long interval = 250;
        Subscription s3 = Observable.interval(interval, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        long position = currentInfo.position;
                        if (currentInfo.positionSynced) {
                            currentInfo.positionSynced = false;
                        } else {
                            position += interval + 10;
                            currentInfo.position = position;
                        }
                        long duration = currentInfo.duration;
                        if (position < 0 || duration <= 0) {
                            setProgress(1000);
                        } else {
                            setProgress((int) (1000 * position / duration));
                        }
                    }
                });
        broadcastSubscriptions = new CompositeSubscription(s, s2, s3);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }

}
