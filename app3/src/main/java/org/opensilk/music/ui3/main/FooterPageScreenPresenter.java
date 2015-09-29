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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.view.View;

import org.opensilk.common.core.dagger2.SubScreenScope;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.nowplaying.NowPlayingActivity;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;

/**
 * Created by drew on 9/17/15.
 */
@SubScreenScope
public class FooterPageScreenPresenter extends ViewPresenter<FooterPageScreenView> {

    final ArtworkRequestManager artworkReqestor;
    final QueueItem queueItem;
    final AppPreferences settings;
    final PlaybackController playbackController;
    final DrawerOwner drawerController;


    CompositeSubscription subscriptions = new CompositeSubscription();
    PlaybackStateCompat lastState;

    @Inject
    public FooterPageScreenPresenter(
            ArtworkRequestManager artworkReqestor,
            QueueItem queueItem,
            AppPreferences settings,
            PlaybackController playbackController,
            DrawerOwner drawerController
    ) {
        this.artworkReqestor = artworkReqestor;
        this.queueItem = queueItem;
        this.settings = settings;
        this.playbackController = playbackController;
        this.drawerController = drawerController;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscriptions.clear();
        MediaDescriptionCompat desc = queueItem.getDescription();
        getView().trackTitle.setText(desc.getTitle());
        getView().artistName.setText(desc.getSubtitle());
        Bitmap artwork = desc.getIconBitmap();
        Bundle extras = desc.getExtras();
        ArtInfo artInfo = null;
        if (extras != null) {
            artInfo = BundleHelper.getParcelable(extras);
        }
        if (artwork != null) {
            getView().artworkThumbnail.setImageBitmap(artwork);
        } else if (artInfo != null) {
            Subscription s = artworkReqestor.newRequest(getView().artworkThumbnail, null, artInfo, ArtworkType.THUMBNAIL);
            subscriptions.add(s);
        } else {
            getView().artworkThumbnail.setDefaultImage(R.drawable.default_artwork);
        }
        updatePlayBtn();
        subscribeBroadcasts();
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        subscriptions.clear();
    }

    void togglePlayback() {
        playbackController.playorPause();
    }

    void onClick(View view) {
        handleClick(settings.getString(AppPreferences.FOOTER_CLICK,
                AppPreferences.ACTION_OPEN_NOW_PLAYING), view);
    }

    boolean onLongClick(View view) {
        return handleClick(settings.getString(AppPreferences.FOOTER_LONG_CLICK,
                AppPreferences.ACTION_NONE), view);
    }

    boolean handleClick(String action, View view) {
        switch (action) {
            case AppPreferences.ACTION_OPEN_QUEUE:
//                NowPlayingActivity.startSelf(view.getContext(), true);
                return true;
            case AppPreferences.ACTION_OPEN_NOW_PLAYING:
//                NowPlayingActivity.startSelf(view.getContext(), false);
                return true;
            case AppPreferences.ACTION_NONE:
            default:
                return false;
        }
    }

    void openControls() {
        drawerController.openDrawer(GravityCompat.END);
    }

    void updatePlayBtn() {
        if (hasView() && lastState != null) {
            getView().setPlaying(PlaybackController.isPlayingOrSimilar(lastState));
        }
    }

    void subscribeBroadcasts() {
        final Subscription s = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        int state = lastState != null ? lastState.getState() : STATE_NONE;
                        lastState = playbackState;
                        if (state != playbackState.getState()) {
                            updatePlayBtn();
                        }
                    }
                }
        );
        subscriptions.add(s);
    }

}
