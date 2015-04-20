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

package org.opensilk.music.ui2.nowplaying;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.utils.MusicUtils;
import com.triggertrap.seekarc.SeekArc;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkProvider;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenDialog;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func4;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class NowPlayingScreenPresenter extends ViewPresenter<NowPlayingView> implements
        PausesAndResumes,
        SeekArc.OnSeekArcChangeListener {

    final Context appContext;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final MusicServiceConnection musicService;
    final ArtworkRequestManager requestor;
    final ActionBarOwner actionBarOwner;
    final EventBus eventBus;
    final AppPreferences settings;

    CompositeSubscription broadcastSubscription;
    Scheduler.Worker timeWorker;

    volatile long posOverride = -1;
    long lastSeekEventTime;
    volatile boolean fromTouch = false;
    volatile boolean isPlaying;
    int sessionId = AudioEffect.ERROR_BAD_VALUE;

    @Inject
    public NowPlayingScreenPresenter(@ForApplication Context appContext,
                                     PauseAndResumeRegistrar pauseAndResumeRegistrar,
                                     MusicServiceConnection musicService,
                                     ArtworkRequestManager requestor,
                                     ActionBarOwner actionBarOwner,
                                     @Named("activity") EventBus eventBus,
                                     AppPreferences settings) {
        this.appContext = appContext;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.musicService = musicService;
        this.requestor = requestor;
        this.actionBarOwner = actionBarOwner;
        this.eventBus = eventBus;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
        getAudioSessionId();
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (pauseAndResumeRegistrar.isRunning()) {
            teardown();
        }
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad()");
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            setup();
        }
        setupActionBar();
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
    }

    @Override
    public void onResume() {
        setup();
        if (getView() != null) {
            getView().attachVisualizer(sessionId);
            getView().setEnabled(isPlaying);
        }
    }

    @Override
    public void onPause() {
        teardown();
        if (getView() != null) {
            getView().destroyVisualizer();
        }
    }

    void setup() {
        startMonitorProgress();
        subscribeBroadcasts();
    }

    void teardown() {
        stopMonitorProgress();
        unsubscribeBroadcasts();
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) return;
        broadcastSubscription = new CompositeSubscription(
                BroadcastObservables.playStateChanged(appContext).subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean playing) {
                        isPlaying = playing;
                        if (getView() == null) return;
                        getView().play.setChecked(playing);
                        getView().setVisualizerEnabled(playing);
                    }
                }),
                BroadcastObservables.trackChanged(appContext)
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                refreshTotalTimeText();
                            }
                        }),
                observeOnMain(BroadcastObservables.artworkChanged(appContext, musicService))
                        .subscribe(new Action1<ArtInfo>() {
                            @Override
                            public void call(ArtInfo artInfo) {
                                if (getView() == null) return;
                                AnimatedImageView v = getView().getArtwork();
                                if (v == null) return;
                                requestor.newAlbumRequest(v, null, artInfo, ArtworkType.LARGE);
                            }
                        }),
                observeOnMain(BroadcastObservables.shuffleModeChanged(appContext, musicService))
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                if (getView() == null) return;
                                getView().shuffle.setImageLevel(integer);
                            }
                        }),
                observeOnMain(BroadcastObservables.repeatModeChanged(appContext, musicService))
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                if (getView() == null) return;
                                getView().repeat.setImageLevel(integer);
                            }
                        })
        );
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) {
            broadcastSubscription.unsubscribe();
            broadcastSubscription = null;
        }
    }

    void getAudioSessionId() {
        observeOnMain(musicService.getAudioSessionId())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer id) {
                        sessionId = id;
                        if (getView() == null) return;
                        getView().attachVisualizer(id);
                    }
                });
    }

    @Override
    public void onProgressChanged(final SeekArc bar, final int progress, final boolean fromuser) {
        if (!fromuser) return;
        final long now = SystemClock.elapsedRealtime();
        observeOnMain(musicService.getDuration())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long duration) {
                        if (now - lastSeekEventTime > 10) {
                            lastSeekEventTime = now;
                            posOverride = duration * progress / 1000;
                            refreshCurrentTimeText(posOverride);
                            if (!fromTouch) {
                                posOverride = -1;
                            }
                        }
                    }
                });
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
        if (posOverride != -1) {
            try {
                musicService.seek(posOverride).subscribe();
            } catch (Exception e) {
            }
        }
        posOverride = -1;
        fromTouch = false;
    }

    void updateProgress(int progress) {
        if (getView() == null) return;
        getView().seekBar.setProgress(progress);
    }

    void refreshTotalTimeText() {
        observeOnMain(musicService.getDuration()).subscribe(
                new SimpleObserver<Long>() {
                    @Override
                    public void onNext(Long duration) {
                        if (getView() == null) return;
                        getView().totalTime.setText(
                                MusicUtils.makeTimeString(getView().getContext(), duration / 1000)
                        );
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                    }
                }
        );
    }

    void refreshCurrentTimeText(final long pos) {
        if (getView() == null) return;
        if (pos >= 0) {
            getView().currentTime.setText(MusicUtils.makeTimeString(getView().getContext(), pos / 1000));
        } else {
            getView().currentTime.setText("--:--");
        }
    }

    void setCurrentTimeVisibile() {
        if (getView() == null) return;
        getView().currentTime.setVisibility(View.VISIBLE);
    }

    void toggleCurrentTimeVisiblility() {
        if (getView() == null) return;
        getView().currentTime.setVisibility(
                getView().currentTime.getVisibility() == View.VISIBLE
                        ? View.INVISIBLE : View.VISIBLE
        );
    }

    void startMonitorProgress() {
        if (timeWorker != null) timeWorker.unsubscribe();
        timeWorker = Schedulers.newThread().createWorker();
        timeWorker.schedule(new Action0() {
            @Override
            public void call() {
                long nextUpdate = 500;
                try {
                    final long playPos = musicService.getPosition().toBlocking().first();
                    final long playDur = musicService.getDuration().toBlocking().first();
                    if (playPos >= 0 && playDur > 0) {
                        final int progress = (int) (1000 * playPos / playDur);
                        if (!fromTouch) {
                            if (isPlaying) {
                                getView().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshCurrentTimeText(playPos);
                                        updateProgress(progress);
                                        setCurrentTimeVisibile();
                                    }
                                });
                            } else {
                                // blink the counter
                                getView().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshCurrentTimeText(playPos);
                                        updateProgress(progress);
                                        toggleCurrentTimeVisiblility();
                                    }
                                });
                            }
                        }
                    } else {
                        getView().post(new Runnable() {
                            @Override
                            public void run() {
                                refreshCurrentTimeText(-1);
                                updateProgress(1000);
                            }
                        });
                    }
                    // calculate the number of milliseconds until the next full second,
                    // so
                    // the counter can be updated at just the right time
                    //nextUpdate = 1000 - pos % 1000;
                } catch (final Exception ignored) {
                    nextUpdate = 500;
                }
                if (timeWorker != null && !timeWorker.isUnsubscribed()) {
                    timeWorker.schedule(this, nextUpdate, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    void stopMonitorProgress() {
        timeWorker.unsubscribe();
        timeWorker = null;
    }

    void setupActionBar() {
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
    }

}
