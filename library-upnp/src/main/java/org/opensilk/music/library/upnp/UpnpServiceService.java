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

package org.opensilk.music.library.upnp;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.control.ActionResponseMessage;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.transport.impl.DatagramIOImpl;
import org.fourthline.cling.transport.impl.RecoveringSOAPActionProcessorImpl;
import org.fourthline.cling.transport.spi.DatagramIO;
import org.fourthline.cling.transport.spi.SOAPActionProcessor;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.upnp.provider.UpnpCDUris;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import hugo.weaving.DebugLog;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 6/8/14.
 */
public class UpnpServiceService extends AndroidUpnpServiceImpl {

    String mUpnpCDAuthority;
    UpnpRegistryListener mRegistryListener;
    volatile Scheduler.Worker mShutdownWorker;
    boolean mStarted;

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();
        // Fix the logging integration between java.util.logging and Android internal logging
        org.seamless.util.logging.LoggingUtil.resetRootHandler(
                new org.seamless.android.FixedAndroidLogHandler()
        );
        // enable logging as needed for various categories of Cling:
        Logger.getLogger("org.fourthline.cling").setLevel(Level.INFO);//.FINE);
        Logger.getLogger("org.fourthline.cling.transport.spi.DatagramProcessor").setLevel(Level.INFO);
        Logger.getLogger("org.fourthline.cling.transport.spi.DatagramIO").setLevel(Level.INFO);
        Logger.getLogger("org.fourthline.cling.protocol.ProtocolFactory").setLevel(Level.INFO);
        Logger.getLogger("org.fourthline.cling.model.message.UpnpHeaders").setLevel(Level.INFO);
//            Logger.getLogger("org.fourthline.cling.transport.spi.SOAPActionProcessor").setLevel(Level.FINER);

        UpnpLibraryComponent cmp = DaggerService.getDaggerComponent(getApplicationContext());
        mUpnpCDAuthority = cmp.unpnAuthority();
        mRegistryListener = new UpnpRegistryListener();
        binder.getRegistry().addListener(mRegistryListener);
//        binder.getControlPoint().search();
    }

    @Override
    public void onDestroy() {
        binder.getRegistry().removeListener(mRegistryListener);
        super.onDestroy();
        cancelServiceShutdown();
    }

    @Override
    @DebugLog
    public IBinder onBind(Intent intent) {
        final Registry registry = binder.getRegistry();
        if (registry.isPaused()) {
            registry.resume();
        }
        cancelServiceShutdown();
        pokeService();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        scheduleServiceShutdown();
        return super.onUnbind(intent);
    }

    @Override
    @DebugLog
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStarted = true;
        return START_NOT_STICKY;
    }

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {

            @Override
            public ServiceType[] getExclusiveServiceTypes() {
                return new ServiceType[] {
                        new UDAServiceType("ContentDirectory", 1)
                };
            }

            @Override
            protected SOAPActionProcessor createSOAPActionProcessor() {
                return new RecoveringSOAPActionProcessorImpl() {
                    @Override
                    @DebugLog
                    public void readBody(ActionResponseMessage responseMsg, ActionInvocation actionInvocation) throws UnsupportedDataException {
                        try {
                            super.readBody(responseMsg, actionInvocation);
                        } catch (Exception e) {
                            //Hack for X_GetFeatureList embedding this in the body
                            String fixedBody = StringUtils.remove(getMessageBody(responseMsg),"<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                            responseMsg.setBody(fixedBody);
                            super.readBody(responseMsg, actionInvocation);
                        }
                    }
                };
            }

            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return 2500;//10000;
            }
        };
    }

    class UpnpRegistryListener extends DefaultRegistryListener {
        @Override public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
//            notifyUri(device.getIdentity().getUdn().getIdentifierString());
        }
        @Override public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
//            notifyUri(device.getIdentity().getUdn().getIdentifierString());
        }
        @Override public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            notifyUri(device.getIdentity().getUdn().getIdentifierString());
        }
        @DebugLog
        void notifyUri(String deviceId) {
            getContentResolver().notifyChange(UpnpCDUris.makeUri(mUpnpCDAuthority, deviceId, null), null);
            getContentResolver().notifyChange(LibraryUris.rootUri(mUpnpCDAuthority), null);
        }
    }

    private void pokeService() {
        if (!mStarted) {
            startService(new Intent(this, UpnpServiceService.class));
            mStarted = true;
        }
    }

    private void cancelServiceShutdown() {
        if (mShutdownWorker != null) {
            mShutdownWorker.unsubscribe();
            mShutdownWorker = null;
        }
    }

    private void scheduleServiceShutdown() {
        cancelServiceShutdown();
        mShutdownWorker = Schedulers.computation().createWorker();
        mShutdownWorker.schedule(new Action0() {
            @Override
            public void call() {
                AndroidUpnpService service = binder;
                if (!service.getRegistry().isPaused()) {
                    service.getRegistry().pause();
                }
            }
        }, 10, TimeUnit.MINUTES);
        mShutdownWorker.schedule(new Action0() {
            @Override
            public void call() {
                stopSelf();
            }
        }, 20, TimeUnit.MINUTES);
    }
}
