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
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.playback.PlaybackConstants;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.nowplaying.NowPlayingActivity;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.*;
import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.notSubscribed;

/**
 * Created by drew on 4/20/15.
 */
public class MainPresenter extends ViewPresenter<MainView> implements PausesAndResumes {

    final Context appContext;
    final PlaybackController playbackController;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final AppPreferences settings;

    Subscription playstateSubscription;

    @Inject
    protected MainPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope(%s)", scope);
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
    }

    @Override
    protected void onExitScope() {
        Timber.v("onExitScope()");
        super.onExitScope();
    }

    @Override
    public void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad(%s)", savedInstanceState);
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            subscribeBroadcasts();
        }
    }

    @Override
    public void onSave(Bundle outState) {
        Timber.v("onSave(%s)", outState);
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
        Timber.v("onPause()");
        unsubscribeBroadcasts();
    }

    void updateFabPlay(boolean playing) {
        if (hasView()) {
            getView().fabPlay.setChecked(playing);
        }
    }

    void updateFabShuffle(int shufflemode) {
        if (hasView()) {
            getView().fabShuffle.setImageLevel(shufflemode);
        }
    }

    void updateFabRepeat(int repeatmode) {
        if (hasView()) {
            switch (repeatmode) {
                case PlaybackConstants.REPEAT_NONE:
                    getView().fabRepeat.setImageLevel(0);
                    break;
                case PlaybackConstants.REPEAT_CURRENT:
                    getView().fabRepeat.setImageLevel(1);
                    break;
                case PlaybackConstants.REPEAT_ALL:
                    getView().fabRepeat.setImageLevel(2);
                    break;
            }
        }

    }

    void handlePrimaryAction(String event, String def) {
        String pref = settings.getString(event, def);
        switch (pref) {
            case AppPreferences.ACTION_PLAYPAUSE:
                playbackController.playorPause();
                break;
            case AppPreferences.ACTION_QUICK_CONTROLS:
                if (getView() != null) {
                    getView().toggleSecondaryFabs();
                }
                break;
            case AppPreferences.ACTION_OPEN_NOW_PLAYING:
                if (getView() != null) {
                    NowPlayingActivity.startSelf(getView().getContext(), false);
                }
                break;
            case AppPreferences.ACTION_OPEN_QUEUE:
                if (getView() != null) {
                    NowPlayingActivity.startSelf(getView().getContext(), true);
                }
                break;
            case AppPreferences.ACTION_NONE:
            default:
                break;
        }
    }

    void handlePrimaryClick() {
        handlePrimaryAction(AppPreferences.FAB_CLICK, AppPreferences.ACTION_PLAYPAUSE);
    }

    void handlePrimaryDoubleClick() {
        handlePrimaryAction(AppPreferences.FAB_DOUBLE_CLICK, AppPreferences.ACTION_QUICK_CONTROLS);
    }

    void handlePrimaryLongClick() {
        handlePrimaryAction(AppPreferences.FAB_LONG_CLICK, AppPreferences.ACTION_QUICK_CONTROLS);
    }

    void handlePrimaryFling() {
        handlePrimaryAction(AppPreferences.FAB_FLING, AppPreferences.ACTION_OPEN_NOW_PLAYING);
    }


    void subscribeBroadcasts() {
        if (notSubscribed(playstateSubscription)) {
            playstateSubscription = playbackController.subscribePlayStateChanges(
                    new Action1<PlaybackStateCompat>() {
                        @Override
                        public void call(PlaybackStateCompat playbackState) {
                            updateFabPlay(isPlayingOrSimilar(playbackState));
                            //TODO repeat/shuffle
                        }
                    }
            );
        }
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(playstateSubscription)) {
            playstateSubscription.unsubscribe();
            playstateSubscription = null;
        }
    }

    public static boolean isActive(PlaybackStateCompat state) {
        switch (state.getState()) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_BUFFERING:
            case STATE_CONNECTING:
            case STATE_PLAYING:
                return true;
        }
        return false;
    }

    public static boolean isPlayingOrSimilar(PlaybackStateCompat state) {
        switch (state.getState()) {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_PLAYING:
                return true;
        }
        return false;
    }

    public static boolean isPlaying(PlaybackStateCompat state) {
        return state.getState() == STATE_PLAYING;
    }

}
