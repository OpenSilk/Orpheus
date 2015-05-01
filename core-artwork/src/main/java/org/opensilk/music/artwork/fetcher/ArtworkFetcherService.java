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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IBinder;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarService;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.provider.ArtworkComponent;
import org.opensilk.music.model.ArtInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.MortarScope;

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
        String ARTTYPE = "arttype";
    }

    public static void newTask(Context context, Uri uri, ArtInfo artInfo, ArtworkType type) {
        Intent i = new Intent(context, ArtworkFetcherService.class)
                .setAction(ArtworkFetcherService.ACTION.NEWTASK)
                .setData(uri)
                .putExtra(ArtworkFetcherService.EXTRA.ARTINFO, artInfo)
                .putExtra(ArtworkFetcherService.EXTRA.ARTTYPE, type.toString());
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

    @Inject @Named("fetcher") HandlerThread mHandlerThread;
    @Inject ArtworkFetcherHandler mHandler;

    final List<Integer> mStartIds = new ArrayList<>();
    int mLastStartId = -1;

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
        DaggerService.<ArtworkFetcherComponent>getDaggerComponent(this).inject(this);
        //mHandlerThread.start(); //started by inject
        mHandler.setService(this);
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
        return null;
    }

    @Override
    @DebugLog
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mStartIds) {
            mStartIds.add(startId);
            mLastStartId = startId;
        }
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

    // We have the potential to finish out of order
    // so we only stop the service if everyone has finished
    void maybeStopSelf(int startId) {
        synchronized (mStartIds) {
            mStartIds.remove(startId);
            if (mStartIds.isEmpty()) {
                stopSelf(mLastStartId);
            }
        }
    }
}
