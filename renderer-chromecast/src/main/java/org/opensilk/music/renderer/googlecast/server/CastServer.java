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

package org.opensilk.music.renderer.googlecast.server;

import android.net.wifi.WifiManager;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.opensilk.music.renderer.googlecast.BuildConfig;
import org.opensilk.music.renderer.googlecast.CastRendererScope;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 10/29/15.
 */
@CastRendererScope
public class CastServer extends Server {
    public static final boolean DUMP_REQUEST_HEADERS = BuildConfig.DEBUG;

    public static final int SERVER_PORT = 40959;

    private final WifiManager.WifiLock mWifiLock;

    @Inject
    public CastServer(
            LocalHandler mLocalHandler,
            ProxyHandler mProxyHandler,
            ArtHandler mArtHandler,
            RootHandler mRootHandler,
            WifiManager mWifiManager
    ) {
        super(SERVER_PORT);
        ContextHandler localContext = new ContextHandler("/track/local");
        localContext.setHandler(mLocalHandler);
        ContextHandler proxyContext = new ContextHandler("/track/proxy");
        proxyContext.setHandler(mProxyHandler);
        ContextHandler artContext = new ContextHandler("/artwork");
        artContext.setHandler(mArtHandler);
        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        handlerCollection.setHandlers(new Handler[]{
                localContext,
                proxyContext,
                artContext,
                mRootHandler,
        });
        setHandler(handlerCollection);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(2);
        threadPool.setMaxThreads(16);
        threadPool.setMaxIdleTimeMs(3 * 60000);
        setThreadPool(threadPool);
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "CastServer");
    }

    @Override
    protected void doStart() throws Exception {
        Timber.d("Starting CastServer");
        super.doStart();
        mWifiLock.acquire();
    }

    @Override
    protected void doStop() throws Exception {
        Timber.d("Stopping CastServr");
        super.doStop();
        mWifiLock.release();
    }
}
