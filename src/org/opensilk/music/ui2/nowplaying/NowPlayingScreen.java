/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.SeekBar;

import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.mortar.PausesAndResumes;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenDialog;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 11/17/14.
 */
@Layout(R.layout.now_playing)
@WithModule(NowPlayingScreen.Module.class)
public class NowPlayingScreen extends Screen {

    @dagger.Module(
            injects = NowPlayingView.class,
            complete = false
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<NowPlayingView> implements
            PausesAndResumes,
            SeekBar.OnSeekBarChangeListener {

        final Context appContext;
        final PauseAndResumeRegistrar pauseAndResumeRegistrar;
        final MusicServiceConnection musicService;
        final ArtworkRequestManager requestor;
        final ActionBarOwner actionBarOwner;
        final EventBus eventBus;
        final AppPreferences settings;

        CompositeSubscription broadcastSubscription;
        Scheduler.Worker timeWorker;
        
        volatile long mPosOverride = -1;
        long mStartSeekPos = 0;
        long mLastSeekEventTime;
        long mLastShortSeekEventTime;
        volatile boolean mFromTouch = false;
        volatile boolean mIsPlaying;

        @Inject
        public Presenter(@ForApplication Context appContext,
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
        }

        @Override
        public void onPause() {
            teardown();
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
                            mIsPlaying = playing;
                            if (getView() == null) return;
                            getView().play.setChecked(playing);
                        }
                    }),
                    BroadcastObservables.trackChanged(appContext).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            if (getView() == null) return;
                            getView().toolbar.setTitle(s);
                            refreshTotalTimeText();
                        }
                    }),
                    BroadcastObservables.artistChanged(appContext).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            if (getView() == null) return;
                            getView().toolbar.setSubtitle(s);
                        }
                    }),
                    observeOnMain(BroadcastObservables.artworkChanged(appContext, musicService))
                            .subscribe(new Action1<ArtInfo>() {
                                @Override
                                public void call(ArtInfo artInfo) {
                                    if (getView() == null) return;
                                    requestor.newAlbumRequest(getView().artwork,
                                            paletteObserver, artInfo, ArtworkType.LARGE);
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

        @Override
        public void onProgressChanged(final SeekBar bar, final int progress, final boolean fromuser) {
            if (!fromuser) return;
            final long now = SystemClock.elapsedRealtime();
            if (now - mLastSeekEventTime > 250) {
                mLastSeekEventTime = now;
                mLastShortSeekEventTime = now;
                try {
                    mPosOverride = musicService.getDuration().toBlocking().first() * progress / 1000;
                    musicService.seek(mPosOverride);
                } catch (Exception e) {

                }
                if (!mFromTouch) {
                    // refreshCurrentTime();
                    mPosOverride = -1;
                }
            } else if (now - mLastShortSeekEventTime > 5) {
                mLastShortSeekEventTime = now;
                try {
                    mPosOverride = musicService.getDuration().toBlocking().first() * progress / 1000;
                    refreshCurrentTimeText(mPosOverride);
                } catch (Exception e) {

                }
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
            setCurrentTimeVisibile();
        }

        @Override
        public void onStopTrackingTouch(final SeekBar bar) {
            if (mPosOverride != -1) {
                try {
                    musicService.seek(mPosOverride).subscribe();
                } catch (Exception e) {}
            }
            mPosOverride = -1;
            mFromTouch = false;
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
                        final long pos = mPosOverride < 0 ? playPos : mPosOverride;
                        if (pos >= 0 && playDur > 0) {
                            final int progress = (int) (1000 * pos / playDur);
                            if (!mFromTouch) {
                                if (mIsPlaying) {
                                    getView().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshCurrentTimeText(pos);
                                            updateProgress(progress);
                                            setCurrentTimeVisibile();
                                        }
                                    });
                                } else {
                                    // blink the counter
                                    getView().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshCurrentTimeText(pos);
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
                    timeWorker.schedule(this, nextUpdate, TimeUnit.MILLISECONDS);
                }
            });
        }

        void stopMonitorProgress() {
            timeWorker.unsubscribe();
            timeWorker = null;
        }

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                .setUpButtonEnabled(true)
                .setMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                    .withMenus(R.menu.popup_share,
                            R.menu.popup_set_ringtone,
                            R.menu.popup_delete
                    )
                    .setActionHandler(new Func1<Integer, Boolean>() {
                        @Override
                        public Boolean call(Integer integer) {
                            switch (integer) {
                                case R.id.popup_menu_share:
                                    //TODO
                                    eventBus.post(new MakeToast(R.string.err_generic));
                                    return true;
                                case R.id.popup_set_ringtone:
                                    musicService.getTrackId().map(new Func1<Long, Long>() {
                                            @Override
                                            public Long call(Long id) {
                                                return MusicProviderUtil.getRealId(appContext, id);
                                            }
                                        })
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new SimpleObserver<Long>() {
                                            @Override
                                            public void onNext(Long id) {
                                                MusicUtils.setRingtone(appContext, id);
                                            }
                                            @Override
                                            public void onError(Throwable e) {
                                                super.onError(e);
                                                eventBus.post(new MakeToast(R.string.err_generic));
                                            }
                                        });
                                    return true;
                                case R.id.popup_delete:
                                    Observable.zip(
                                        musicService.getTrackName(),
                                        musicService.getTrackId().map(new Func1<Long, Long>() {
                                            @Override
                                            public Long call(Long id) {
                                                return MusicProviderUtil.getRealId(appContext, id);
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
                                                eventBus.post(new MakeToast(R.string.err_generic));
                                            }
                                        });
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    }).build()
                ).build()
            );
        }

        final PaletteObserver paletteObserver = new PaletteObserver() {
            @Override
            public void onNext(PaletteResponse paletteResponse) {
                if (getView() == null) return;
                getView().colorize(paletteResponse.palette);
            }
        };

    }

}
