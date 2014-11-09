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

package org.opensilk.music.ui2.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.andrew.apollo.MusicPlaybackService;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.MusicServiceConnection;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 11/7/14.
 */
public class BroadcastObservables {

    public static Observable<Boolean> playStateChanged(Context appContext) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        Observable<Intent> intentObservable = AndroidObservable.fromBroadcast(appContext, intentFilter);
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
        Observable<Intent> intentObservable = AndroidObservable.fromBroadcast(appContext, intentFilter);
        return intentObservable;
    }

    public static Observable<Long> trackIdChanged(Context appContext) {
        return metaChanged(appContext).map(new Func1<Intent, Long>() {
            @Override
            public Long call(Intent intent) {
                return intent.getLongExtra("id", -1);
            }
        });
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
        //TODO test and see if we really need to push these to io, i think we dont
        return metaChanged(appContext).observeOn(Schedulers.io()).flatMap(new Func1<Intent, Observable<ArtInfo>>() {
            @Override
            public Observable<ArtInfo> call(Intent intent) {
                return connection.getCurrentArtInfo();
            }
        });
    }

    public static Observable<Integer> shuffleModeChanged(Context appContext, final MusicServiceConnection connection) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.io());
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
        Observable<Intent> intentObservable = AndroidObservable
                .fromBroadcast(appContext, intentFilter).observeOn(Schedulers.io());
        return intentObservable.flatMap(new Func1<Intent, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Intent intent) {
                return connection.getRepeatMode();
            }
        });
    }

    public static Observable<Intent> queueChanged(Context appContext) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlaybackService.QUEUE_CHANGED);
        return AndroidObservable.fromBroadcast(appContext, intentFilter);
    }

}
