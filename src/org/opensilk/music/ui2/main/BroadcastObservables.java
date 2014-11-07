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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 11/7/14.
 */
public class BroadcastObservables {

    public static Observable<Boolean> playStateChanged(Context appContext) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // obr will call onNext on the main thread so we observeOn computation
        // so our chained operators will be called on computation instead of main.
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.computation());
        return intentObservable.map(new Func1<Intent, Boolean>() {
            @Override
            public Boolean call(Intent intent) {
                return intent.getBooleanExtra("playing", false);
            }
        });
    }

    public static Observable<Intent> metaChanged(Context appContext) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.META_CHANGED);
        // obr will call onNext on the main thread so we observeOn computation
        // so our chained operators will be called on computation instead of main.
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.computation());
        return intentObservable;
    }

    public static Observable<String> trackChanged(Context appContext) {
        return metaChanged(appContext).map(new Func1<Intent, String>() {
            @Override
            public String call(Intent intent) {
                return intent.getStringExtra("track");
            }
        });
    }

    public static Observable<String> artistChanged(Context appContext) {
        return metaChanged(appContext).map(new Func1<Intent, String>() {
            @Override
            public String call(Intent intent) {
                return intent.getStringExtra("artist");
            }
        });
    }

    public static Observable<String> albumChanged(Context appContext) {
        return metaChanged(appContext).map(new Func1<Intent, String>() {
            @Override
            public String call(Intent intent) {
                return intent.getStringExtra("album");
            }
        });
    }

    public static Observable<ArtInfo> artworkChanged(Context appContext, final MusicServiceConnection connection) {
        return metaChanged(appContext).flatMap(new Func1<Intent, Observable<ArtInfo>>() {
            @Override
            public Observable<ArtInfo> call(Intent intent) {
                return connection.getCurrentArtInfo();
            }
        });
    }

    public static Observable<Integer> shuffleModeChanged(Context appContext, final MusicServiceConnection connection) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        // obr will call onNext on the main thread so we observeOn computation
        // so our chained operators will be called on computation instead of main.
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.computation());
        return intentObservable.flatMap(new Func1<Intent, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Intent intent) {
                return connection.getShuffleMode();
            }
        });
    }

    public static Observable<Integer> repeatModeChanged(Context appContext, final MusicServiceConnection connection) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // obr will call onNext on the main thread so we observeOn computation
        // so our chained operators will be called on computation instead of main.
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.computation());
        return intentObservable.flatMap(new Func1<Intent, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Intent intent) {
                return connection.getRepeatMode();
            }
        });
    }

}
