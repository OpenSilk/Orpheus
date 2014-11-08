/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui2.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import timber.log.Timber;

/**
 * Created by drew on 10/11/14.
 */
@Singleton
public class PluginConnectionManager {

    class Token implements ServiceConnection {
        final AsyncSubject<RemoteLibrary> subject;

        Token(AsyncSubject<RemoteLibrary> subject) {
            this.subject = subject;
        }

        @Override
        @DebugLog
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteLibrary library = RemoteLibrary.Stub.asInterface(service);
            subject.onNext(library);
            subject.onCompleted();
        }

        @Override
        @DebugLog
        public void onServiceDisconnected(ComponentName name) {
            onException(name);
        }
    }

    final Context context;
    final Map<ComponentName, Token> connections = new LinkedHashMap<>();

    @Inject
    public PluginConnectionManager(@ForApplication Context context) {
        this.context = new ContextWrapper(context);
    }

    public synchronized Observable<RemoteLibrary> bind(ComponentName componentName) {
        if (connections.containsKey(componentName)) {
            return connections.get(componentName).subject.asObservable();
        } else {
            Timber.v("Binding %s", componentName);
            AsyncSubject<RemoteLibrary> subject = AsyncSubject.create();
            Token token = new Token(subject);
            connections.put(componentName, token);
            context.startService(new Intent().setComponent(componentName));
            context.bindService(new Intent().setComponent(componentName), token, 0);
            return subject.asObservable();
        }
    }

    public synchronized void onPause() {
        for (final Map.Entry<ComponentName, Token> entry : connections.entrySet()) {
            entry.getValue().subject.observeOn(Schedulers.io()).subscribe(new Action1<RemoteLibrary>() {
                @Override
                public void call(RemoteLibrary remoteLibrary) {
                    Timber.v("pause(%s) called on %s", entry.getKey(), Thread.currentThread().getName());
                    try {
                        remoteLibrary.pause();
                    } catch (RemoteException e) {
                        Timber.e("pause(%s) failed", entry.getKey());
                        onException(entry.getKey());
                    }
                }
            });
        }
    }

    public synchronized void onResume() {
        for (final Map.Entry<ComponentName, Token> entry : connections.entrySet()) {
            entry.getValue().subject.observeOn(Schedulers.io()).subscribe(new Action1<RemoteLibrary>() {
                @Override
                public void call(RemoteLibrary remoteLibrary) {
                    Timber.v("resume(%s) called on %s", entry.getKey(), Thread.currentThread().getName());
                    try {
                        remoteLibrary.resume();
                    } catch (RemoteException e) {
                        Timber.e("resume(%s) failed", entry.getKey());
                        onException(entry.getKey());
                    }
                }
            });
        }
    }

    public synchronized void onDestroy() {
        for (final Map.Entry<ComponentName, Token> entry : connections.entrySet()) {
            entry.getValue().subject.subscribe(new Action1<RemoteLibrary>() {
                @Override
                public void call(RemoteLibrary remoteLibrary) {
                    Timber.v("unbinding(%s) called on %s", entry.getKey(), Thread.currentThread().getName());
                    context.unbindService(entry.getValue());
                }
            });
        }
        connections.clear();
    }

    public synchronized void onException(ComponentName componentName) {
        final Token token = connections.remove(componentName);
        Timber.v("Unbinding %s", componentName);
        if (token == null) return;
        //TODO does unbind have to be called from main thread?
        if (Looper.getMainLooper() == Looper.myLooper()) {
            context.unbindService(token);
        } else {
            final Scheduler.Worker w = AndroidSchedulers.mainThread().createWorker();
            w.schedule(new Action0() {
                @Override
                public void call() {
                    context.unbindService(token);
                    w.unsubscribe();
                }
            });
        }
    }

}
