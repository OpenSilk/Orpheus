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

package com.andrew.apollo;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.rx.SingleThreadScheduler;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.AsyncSubject;
import timber.log.Timber;

/**
 * Created by drew on 4/22/15.
 */
public class PlaybackManager {

    private class Token implements ServiceConnection {
        final AsyncSubject<IApolloService> subject;

        private Token(AsyncSubject<IApolloService> subject) {
            this.subject = subject;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final IApolloService connection = IApolloService.Stub.asInterface(service);
            subject.onNext(connection);
            subject.onCompleted();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbind();
        }
    }

    static final Scheduler SCHEDULER = new SingleThreadScheduler();

    private final Context context;
    // protected by synchronized methods
    private Token serviceToken;

    @Inject
    public PlaybackManager(@ForApplication Context context) {
        Timber.v("new MusicServiceConnection");
        this.context = new ContextWrapper(context);
    }

    public synchronized void bind() {
        if (serviceToken == null) {
            Timber.d("binding MusicService");
            AsyncSubject<IApolloService> subject = AsyncSubject.create();
            serviceToken = new Token(subject);
            context.startService(new Intent(context, MusicPlaybackService.class));
            context.bindService(new Intent(context, MusicPlaybackService.class), serviceToken, 0);
        }
    }

    public synchronized void unbind() {
        if (serviceToken == null) return;
        Timber.d("unbinding MusicService");
        context.unbindService(serviceToken);
        serviceToken = null;
    }

    void onError(Exception e) {
        Timber.w(e, "MusicServiceConnection");
        unbind();
    }

    void onRemoteException(RemoteException e) {
        Timber.w(e, "MusicServiceConnection");
        unbind();
    }

    Observable<IApolloService> getObservable() {
        bind();
        // NOTE: onServiceConnected() is called from main thread
        // hence the onNext() in the subject is called from main thread
        // for this reason we 'observe' the onNextCall on an IO thread.
        // so when the functions will receive the Func1.call() in the flatMap
        // on the IO thread not the main thread.
        return serviceToken.subject.asObservable().first().observeOn(SCHEDULER);
    }

    public interface Callback {
        void onStateChanged();
    }

    public void play(Uri uri) {

    }

    public void playAll(long[] list) {

    }
}
