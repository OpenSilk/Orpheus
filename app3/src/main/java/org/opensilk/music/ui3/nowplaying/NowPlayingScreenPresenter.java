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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.graphics.Palette;
import android.view.View;

import com.triggertrap.seekarc.SeekArc;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.main.MainPresenter;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.*;
import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.music.playback.PlaybackConstants.META.TRACK_ARTWORK_URI;

/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class NowPlayingScreenPresenter extends ViewPresenter<NowPlayingScreenView> implements
        PausesAndResumes,
        SeekArc.OnSeekArcChangeListener {

    final Context appContext;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final PlaybackController playbackController;
    final ArtworkRequestManager requestor;
//    final ActionBarOwner actionBarOwner;
    final AppPreferences settings;

    CompositeSubscription broadcastSubscription;

    long posOverride = -1;
    long lastSeekEventTime;
    boolean fromTouch = false;
    boolean isPlaying;
    int sessionId = AudioEffect.ERROR_BAD_VALUE;
    long lastPosition = -1;
    long lastDuration = -1;
    boolean lastPosSynced;
    ArtInfo lastArtInfo;
    long lastBlinkTime;

    @Inject
    public NowPlayingScreenPresenter(
            @ForApplication Context appContext,
             PauseAndResumeRegistrar pauseAndResumeRegistrar,
             PlaybackController playbackController,
             ArtworkRequestManager requestor,
//             ActionBarOwner actionBarOwner,
             AppPreferences settings
    ) {
        this.appContext = appContext;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.playbackController = playbackController;
        this.requestor = requestor;
//        this.actionBarOwner = actionBarOwner;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            setup();
        }
        setupActionBar();
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
            teardown();
        }
    }

    @Override
    public void onResume() {
        setup();
    }

    @Override
    public void onPause() {
        teardown();
    }

    @DebugLog
    void setup() {
        sessionId = playbackController.getAudioSessionId();
        if (hasView()) {
            getView().attachVisualizer(sessionId);
            getView().setEnabled(isPlaying);
        }
        subscribeBroadcasts();
    }

    @DebugLog
    void teardown() {
        if (hasView()) {
            getView().destroyVisualizer();
        }
        unsubscribeBroadcasts();
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) return;
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        boolean playing = MainPresenter.isPlaying(playbackState);
                        if (hasView()) {
                            getView().play.setChecked(MainPresenter.isActive(playbackState));
                            getView().setVisualizerEnabled(playing);
                        }
                        isPlaying = playing;
                        long position = playbackState.getPosition();
                        long duration = playbackState.getBufferedPosition();
                        updateProgress(position, duration);
                        if (duration != lastDuration) {
                            setTotalTimeText(duration);
                        }
                        Timber.v("Position discrepancy = %d", lastPosition - position);
                        lastPosition = position;
                        lastDuration = duration;
                        lastPosSynced = true;
                        //TODO shuffle/repeat
                    }
                }
        );
        Subscription s2 = playbackController.subscribeMetaChanges(
                new Action1<MediaMetadataCompat>() {
                    @Override
                    public void call(MediaMetadataCompat mediaMetadata) {
                        //TODO maybe should just set the large art uri in the metadata
                        String track = mediaMetadata.getString(METADATA_KEY_TITLE);
                        String artist = mediaMetadata.getString(METADATA_KEY_ARTIST);
                        String albumArtist = mediaMetadata.getString(METADATA_KEY_ALBUM_ARTIST);
                        String album = mediaMetadata.getString(METADATA_KEY_ALBUM);
                        String uriString = mediaMetadata.getString(METADATA_KEY_ART_URI);
                        Uri artworkUri = uriString != null ? Uri.parse(uriString) : null;
                        Timber.d("%s, %s, %s, %s", artist, albumArtist, album, uriString);
                        final ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(albumArtist, artist, album, artworkUri);
                        Timber.d(artInfo.toString());
                        if (!artInfo.equals(lastArtInfo)) {
                            lastArtInfo = artInfo;
                            if (hasView() && getView().artwork != null) {
                                requestor.newRequest(getView().artwork, getView().paletteObserver, artInfo, ArtworkType.LARGE);
                            }
                        }
                        //Todo update display
                    }
                }
        );
        final long interval = 250;
        Subscription s3 = Observable.interval(interval, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        long position = lastPosition;
                        if (lastPosSynced) {
                            lastPosSynced = false;
                        } else if (isPlaying) {
                            position += interval + 10;
                            lastPosition = position;
                        }
                        long duration = lastDuration;
                        updateProgress(position, duration);
                    }
                });
        broadcastSubscription = new CompositeSubscription(s1, s2, s3);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) {
            broadcastSubscription.unsubscribe();
            broadcastSubscription = null;
        }
    }

    @Override
    public void onProgressChanged(final SeekArc bar, final int progress, final boolean fromuser) {
        if (!fromuser) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now > lastSeekEventTime + 30) {
            lastSeekEventTime = now;
            posOverride = (lastDuration * progress) / 1000;
            if (posOverride < 0 || !fromTouch) {
                posOverride = -1;
            } else {
                setCurrentTimeText(posOverride);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekArc bar) {
        lastSeekEventTime = 0;
        posOverride = -1;
        fromTouch = true;
        setCurrentTimeVisibile();
    }

    @Override
    public void onStopTrackingTouch(final SeekArc bar) {
        fromTouch = false;
        if (posOverride != -1) {
            playbackController.seekTo(posOverride);
            updateProgress(posOverride, lastDuration);
        }
        posOverride = -1;
    }

    void updateProgress(long position, long duration) {
        if (position < 0 || duration <= 0) {
            setProgress(1000);
            setCurrentTimeText(position);
        } else {
            if (!fromTouch) {
                setCurrentTimeText(position);
                setProgress((int) (1000 * position / duration));
                if (isPlaying) {
                    setCurrentTimeVisibile();
                } else {
                    //blink the counter
                    long now = SystemClock.elapsedRealtime();
                    if (now >= lastBlinkTime + 500) {
                        lastBlinkTime = now;
                        toggleCurrentTimeVisiblility();
                    }
                }
            }
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().seekBar.setProgress(progress);
        }
    }

    void setTotalTimeText(long duration) {
        if (hasView()) {
            if (duration > 0) {
                getView().totalTime.setText(UtilsCommon.makeTimeString(getView().getContext(), duration / 1000));
            } else {
                getView().totalTime.setText("--:--");
            }
        }
    }

    void setCurrentTimeText(long pos) {
        if (hasView()) {
            if (pos >= 0) {
                getView().currentTime.setText(UtilsCommon.makeTimeString(getView().getContext(), pos / 1000));
            } else {
                getView().currentTime.setText("--:--");
            }
        }
    }

    void setCurrentTimeVisibile() {
        if (hasView()) {
            getView().currentTime.setVisibility(View.VISIBLE);
        }
    }

    void toggleCurrentTimeVisiblility() {
        if (hasView()) {
            boolean visible = getView().currentTime.getVisibility() == View.VISIBLE;
            getView().currentTime.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
        }
    }

    void setupActionBar() {
        /*
        final Func1<Integer, Boolean> handler = new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer integer) {
                switch (integer) {
                    case R.id.popup_menu_share:
                        Observable.zip(
                                musicService.getTrackName(),
                                musicService.getArtistName(),
                                musicService.getAlbumArtistName(),
                                musicService.getAlbumName(),
                                new Func4<String, String, String, String, String[]>() {
                                    @Override
                                    public String[] call(String s, String s2, String s3, String s4) {
                                        return new String[]{s, s2, s3, s4};
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new SimpleObserver<String[]>() {
                                    @Override
                                    public void onNext(String[] strings) {
                                        if (strings.length != 4
                                                || strings[0] == null
                                                || strings[1] == null
                                                || getView() == null) {
                                            notifyError();
                                        } else {
                                            Intent si = new Intent();
                                            String msg = getView().getContext().getString(
                                                    R.string.now_listening_to, strings[0], strings[1]);
                                            si.setAction(Intent.ACTION_SEND);
                                            si.setType("text/plain");
                                            si.putExtra(Intent.EXTRA_TEXT, msg);
                                            String albumArtist = strings[1];
                                            if (strings[2] != null) {
                                                albumArtist = strings[2];
                                            }
                                            String album = strings[3];
                                            if (albumArtist != null && album != null) {
                                                si.putExtra(Intent.EXTRA_STREAM,
                                                        ArtworkProvider.createArtworkUri(albumArtist, album));
                                            }
                                            getView().getContext().startActivity(
                                                    Intent.createChooser(si,
                                                            getView().getContext().getString(R.string.share_track_using))
                                            );
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        super.onError(e);
                                        notifyError();
                                    }

                                    void notifyError() {
                                        eventBus.post(new MakeToast(R.string.err_generic));
                                    }
                                });
                        return true;
                    case R.id.popup_set_ringtone:
                        musicService.getTrackId()
                                .map(new Func1<Long, Long>() {
                                    @Override
                                    public Long call(Long id) {
                                        long realId = MusicProviderUtil.getRealId(appContext, id);
                                        if (realId < 0) {
                                            throw new IllegalArgumentException("Song not in MediaStore");
                                        }
                                        return realId;
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new SimpleObserver<Long>() {
                                    @Override
                                    public void onNext(Long id) {
                                        MakeToast mt = MusicUtils.setRingtone(appContext, id);
                                        if (mt != null) {
                                            eventBus.post(mt);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        super.onError(e);
                                        int msg = (e instanceof IllegalArgumentException)
                                                ? R.string.err_unsupported_for_library : R.string.err_generic;
                                        eventBus.post(new MakeToast(msg));
                                    }
                                });
                        return true;
                    case R.id.popup_delete:
                        Observable.zip(
                                musicService.getTrackName(),
                                musicService.getTrackId().map(new Func1<Long, Long>() {
                                    @Override
                                    public Long call(Long id) {
                                        long realId = MusicProviderUtil.getRealId(appContext, id);
                                        if (realId < 0) {
                                            throw new IllegalArgumentException("Song not in MediaStore");
                                        }
                                        return realId;
                                    }
                                }), new Func2<String, Long, OpenDialog>() {
                                    @Override
                                    public OpenDialog call(String s, Long id) {
                                        return new OpenDialog(DeleteDialog.newInstance(s, new long[]{id}));
                                    }
                                })
                                .subscribe(new SimpleObserver<OpenDialog>() {
                                    @Override
                                    public void onNext(OpenDialog openDialog) {
                                        eventBus.post(openDialog);
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        super.onError(e);
                                        int msg = (e instanceof IllegalArgumentException)
                                                ? R.string.err_unsupported_for_library : R.string.err_generic;
                                        eventBus.post(new MakeToast(msg));
                                    }
                                });
                        return true;
                    default:
                        return false;
                }
            }
        };
        actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                        .setTitle(R.string.now_playing)
                        .setUpButtonEnabled(true)
                        .setMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                                        .withMenus(
                                                R.menu.popup_share,
                                                R.menu.popup_set_ringtone,
                                                R.menu.popup_delete
                                        )
                                        .setActionHandler(handler).build()
                        ).build()
        );
        */
    }

}
