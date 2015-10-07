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

package org.opensilk.music.artwork.fetcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarService;
import org.opensilk.music.artwork.provider.ArtworkComponent;
import org.opensilk.music.model.ArtInfo;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 4/30/15.
 */
public class ArtworkFetcherService extends MortarService {

    public interface ACTION {
        String NEWTASK = "newtask";
        String CLEARCACHE = "clearcache";
        String RELOADPREFS = "reloadprefs";
    }

    public interface EXTRA {
        String ARTINFO = "artinfo";
    }

    public static void newTask(Context context, Uri uri, ArtInfo artInfo) {
        Intent i = new Intent(context, ArtworkFetcherService.class)
                .setAction(ArtworkFetcherService.ACTION.NEWTASK)
                .setData(uri)
                .putExtra(ArtworkFetcherService.EXTRA.ARTINFO, artInfo)
                ;
        context.startService(i);
    }

    public static void clearCache(Context context) {
        Intent i = new Intent(context, ArtworkFetcherService.class)
                .setAction(ACTION.CLEARCACHE);
        context.startService(i);
    }

    public static void reloadPrefs(Context context) {
        Intent i = new Intent(context, ArtworkFetcherService.class)
                .setAction(ACTION.RELOADPREFS);
        context.startService(i);
    }

    private HandlerThread mHandlerThread;
    @Inject ArtworkFetcherHandler mHandler;

    private final AtomicInteger mTimesStarted = new AtomicInteger(0);
    private final Runnable mStopSelfTask = new StopSelfTask(this);
    private final Binder mBinder = new Binder();

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        builder.withService(DaggerService.DAGGER_SERVICE,
                ArtworkFetcherComponent.FACTORY.call(
                        DaggerService.<ArtworkComponent>getDaggerComponent(getApplicationContext()),
                        this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread("ArtworkFetcher");
        mHandlerThread.start();
        DaggerService.<ArtworkFetcherComponent>getDaggerComponent(this).inject(this);
    }

    @Override
    @DebugLog
    public void onDestroy() {
        super.onDestroy();
        mHandler.onDestroy();
        mHandlerThread.getLooper().quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    @DebugLog
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTimesStarted.incrementAndGet();
        mHandler.removeCallbacks(mStopSelfTask);
        mHandler.processIntent(intent, startId);
        return START_REDELIVER_INTENT;
    }

    @Override
    @DebugLog
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_COMPLETE) {
            // System will kill us very soon anyway maybe the provider
            // can keep living if we commit suicide;
            stopSelf();
        } else if (level >= 15 /*TRIM_MEMORY_RUNNING_CRITICAL*/) {
            mHandler.sendEmptyMessage(ArtworkFetcherHandler.MSG.ON_LOW_MEM);
        }
    }

    public HandlerThread getHandlerThread() {
        return mHandlerThread;
    }

    void maybeStopSelf(int startId) {
        int remaining = mTimesStarted.decrementAndGet();
        Timber.v("finished %d: %d tasks remain", startId, remaining);
        if (remaining == 0) {
            mHandler.postDelayed(mStopSelfTask, 60 * 1000);
        }
    }

    private static final class StopSelfTask implements Runnable {
        final WeakReference<ArtworkFetcherService> service;

        public StopSelfTask(ArtworkFetcherService service) {
            this.service = new WeakReference<ArtworkFetcherService>(service);
        }

        @Override
        public void run() {
            ArtworkFetcherService s = service.get();
            if (s != null) {
                s.stopSelf();
            }
        }
    }

    class Binder extends android.os.Binder implements ArtworkFetcher {
        public void newRequest(ArtInfo artInfo, CompletionListener listener) {
            mHandler.newTask(artInfo, listener);
        }
    }

    public final static class Connection implements Closeable {
        private final Context context;
        private final ServiceConnection serviceConnection;
        private final ArtworkFetcherService.Binder service;
        private Connection(
                Context context,
                ServiceConnection serviceConnection,
                ArtworkFetcherService.Binder service
        ) {
            this.context = context;
            this.serviceConnection = serviceConnection;
            this.service = service;
        }
        @Override public void close() {
            context.unbindService(serviceConnection);
        }
        public ArtworkFetcher getService() {
            return service;
        }
    }

    public static Connection bindService(Context context) throws InterruptedException {
        if (context == null) throw new IllegalArgumentException("Null context");
        ensureNotOnMainThread(context);
        final BlockingQueue<ArtworkFetcherService.Binder> q = new LinkedBlockingQueue<>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;
            @Override public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    q.offer((ArtworkFetcherService.Binder) service);
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };
        boolean isBound = context.bindService(new Intent(context, ArtworkFetcherService.class),
                keyChainServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (!isBound) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new Connection(context, keyChainServiceConnection, q.take());
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }
}
