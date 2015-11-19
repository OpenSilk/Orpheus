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

package org.opensilk.music.library.upnp.provider;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.ConnectionUtils;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.upnp.R;
import org.opensilk.music.library.upnp.UpnpLibraryComponent;
import org.opensilk.music.library.upnp.UpnpServiceService;
import org.opensilk.music.library.upnp.ui.SettingsActivity;
import org.opensilk.music.library.upnp.util.ModelUtil;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Track;
import org.opensilk.upnp.contentdirectory.Feature;
import org.opensilk.upnp.contentdirectory.Features;
import org.opensilk.upnp.contentdirectory.callback.GetFeatureList;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/**
 * Created by drew on 9/17/15.
 */
public class UpnpCDProvider extends LibraryProvider {

    @Inject @Named("UpnpLibraryAuthority") String mAuthority;
    @Inject ConnectivityManager mConnectivityManager;

    UriMatcher mMatcher;

    static final UDAServiceType sCDServiceType = new UDAServiceType("ContentDirectory");

    @Override
    @DebugLog
    public boolean onCreate() {
        UpnpLibraryComponent app = DaggerService.getDaggerComponent(getContext());
        UpnpCDComponent.FACTORY.call(app).inject(this);
        mMatcher = UpnpCDUris.makeMatcher(mAuthority);
        //We have our own thread pool, no need to thread hop the subscriptions
        setScheduler(Schedulers.immediate());
        return super.onCreate();
    }

    @Override
    @DebugLog
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        //noinspection ConstantConditions
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLabel(getContext().getResources().getString(R.string.upnp_name))
                .setSettingsComponent(new ComponentName(getContext(), SettingsActivity.class))
                .build();
    }

    @Override
    protected String getAuthority() {
        return mAuthority;
    }

    @Override
    protected boolean isAvailable() {
        //TODO setting to specify which wifi networks
        return ConnectionUtils.hasWifiOrEthernetConnection(mConnectivityManager) || VersionUtils.isEmulator();
    }

    @Override
    protected Observable<Model> getListObjsObservable(Uri uri, Bundle args) {
        switch (mMatcher.match(uri)) {
            case UpnpCDUris.M_DEVICE_ROOT:
            case UpnpCDUris.M_OBJECT: {
                final String device = uri.getPathSegments().get(0);
                String folder = "";
                if (uri.getPathSegments().size() > 1) {
                    folder = uri.getLastPathSegment();
                }
                return browseFolders(device, folder, true);
            }
            default: {
                return super.getListObjsObservable(uri, args);
            }
        }
    }

    @Override
    protected Observable<Model> getGetObjObservable(Uri uri, Bundle args) {
        switch (mMatcher.match(uri)) {
            case UpnpCDUris.M_OBJECT: {
                final String device = uri.getPathSegments().get(0);
                final String folder = uri.getPathSegments().get(1);
                return browseFolders(device, folder, false);
            }
            default: {
                return super.getGetObjObservable(uri, args);
            }
        }
    }

    //We use Internal method here because we never terminate
    @Override
    protected void listRootsInternal(Uri uri, IBinder binder, Bundle args) {
        Observable.using(
                mConnectionFactory,
                new Func1<UpnpServiceServiceConnection, Observable<List<org.opensilk.music.model.Container>>>() {
                    @Override
                    public Observable<List<org.opensilk.music.model.Container>> call(
                            final UpnpServiceServiceConnection upnpServiceServiceConnection) {
                        final AndroidUpnpService service = upnpServiceServiceConnection.getService();
                        return Observable.create(new Observable.OnSubscribe<List<org.opensilk.music.model.Container>>() {
                            @Override
                            public void call(final Subscriber<? super List<org.opensilk.music.model.Container>> subscriber) {
                                //first send all the devices we know about
                                List<org.opensilk.music.model.Container> devices = new ArrayList<>();
                                for (RemoteDevice rd : service.getRegistry().getRemoteDevices()) {
                                    if (rd.findService(sCDServiceType) != null) {
                                        devices.add(ModelUtil.parseDevice(mAuthority, rd));
                                    }
                                }
                                if (!devices.isEmpty() && !subscriber.isUnsubscribed()) {
                                    subscriber.onNext(devices);
                                }
                                if (subscriber.isUnsubscribed()) {
                                    return;
                                }
                                final RegistryListener listener = new DefaultRegistryListener() {
                                    @Override
                                    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                                        if (device.findService(sCDServiceType) != null) {
                                            Folder f = ModelUtil.parseDevice(mAuthority, device);
                                            if (!subscriber.isUnsubscribed()) {
                                                subscriber.onNext(Collections.singletonList(
                                                        (org.opensilk.music.model.Container) f));
                                            }
                                        }
                                    }
                                };
                                //make sure we remove our listener
                                subscriber.add(Subscriptions.create(new Action0() {
                                    @Override
                                    @DebugLog
                                    public void call() {
                                        service.getRegistry().removeListener(listener);
                                    }
                                }));
                                //post new devices as they come in
                                service.getRegistry().addListener(listener);
                                service.getControlPoint().search(
                                        new UDAServiceTypeHeader(sCDServiceType));
                            }
                        });
                    }
                },
                mConnectionCloseAction
        ).subscribeOn(Schedulers.computation())
                .subscribe(new BundleableSubscriber<org.opensilk.music.model.Container>(binder));
    }

    protected Observable<Model> browseFolders(
            @NonNull final String deviceIdentity,
            @Nullable final String folderIdentity,
            final boolean listing
    ) {
        return Observable.using(
                mConnectionFactory,
                new Func1<UpnpServiceServiceConnection, Observable<? extends Model>>() {
                    @Override
                    public Observable<? extends Model> call(UpnpServiceServiceConnection upnpServiceServiceConnection) {
                        final AndroidUpnpService service = upnpServiceServiceConnection.getService();
                        //first acquire the CDService
                        return Observable.create(new ContentDirectoryOnSubscribe(service, deviceIdentity))
                                //searches should be basically instant on lan so timeout after short period
                                .timeout(5, TimeUnit.SECONDS)
                                //use the CDService to make the browse call
                                .flatMap(new Func1<RemoteService, Observable<Model>>() {
                                    @Override
                                    public Observable<Model> call(final RemoteService remoteService) {
                                        return makeBrowseObservable(folderIdentity,
                                                service, remoteService, listing);
                                    }
                                });
                    }
                },
                mConnectionCloseAction
        );
    }

    Observable<Model> makeBrowseObservable(
            final String folderIdentity,final AndroidUpnpService service,
            final RemoteService remoteService, final boolean listing) {
        if (StringUtils.isEmpty(folderIdentity)) {
            //first try to get the music only virtual folder, then do the browse
            return Observable.create(new GetFeatureListOnSubscribe(service, remoteService)).flatMap(
                    new Func1<String, Observable<Model>>() {
                        @Override
                        public Observable<Model> call(String s) {
                            return Observable.create(new BrowseOnSubscribe(
                                    mAuthority, service, remoteService, s,
                                    BrowseFlag.DIRECT_CHILDREN));
                        }
                    }
            );
        } else {
            //we were given a folder, browse it
            return Observable.create(new BrowseOnSubscribe(
                    mAuthority, service, remoteService, folderIdentity,
                    listing ? BrowseFlag.DIRECT_CHILDREN : BrowseFlag.METADATA));
        }
    }

    //Fetches the content directory service from the server
    static class ContentDirectoryOnSubscribe implements Observable.OnSubscribe<RemoteService> {
        final AndroidUpnpService upnpService;
        final String deviceIdentity;

        public ContentDirectoryOnSubscribe(AndroidUpnpService upnpService, String deviceIdentity) {
            this.upnpService = upnpService;
            this.deviceIdentity = deviceIdentity;
        }

        @Override
        public void call(final Subscriber<? super RemoteService> subscriber) {
            final UDN udn = UDN.valueOf(deviceIdentity);
            //check cache first
            final RemoteDevice rd = upnpService.getRegistry().getRemoteDevice(udn, false);
            if (rd != null) {
                final RemoteService rs = rd.findService(sCDServiceType);
                if (rs != null) {
                    subscriber.onNext(rs);
                    subscriber.onCompleted();
                    return;
                }
            }
            //missed cache, we have to look it up
            final RegistryListener listener = new DefaultRegistryListener() {
                AtomicBoolean once = new AtomicBoolean(true);
                @Override
                public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                    if (udn.equals(device.getIdentity().getUdn())) {
                        final RemoteService rs = device.findService(sCDServiceType);
                        if (rs != null && once.compareAndSet(true, false)) {
                            subscriber.onNext(rs);
                            subscriber.onCompleted();
                        }
                    }
                }
            };
            //ensure we don't leak our listener
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    upnpService.getRegistry().removeListener(listener);
                }
            }));
            //register listener
            upnpService.getRegistry().addListener(listener);
            Timber.d("Sending a new search for %s", udn);
//            upnpService.getControlPoint().search(new UDNHeader(udn));//doesnt work
            upnpService.getControlPoint().search(new UDAServiceTypeHeader(sCDServiceType));
        }
    }

    //Sends a GetFeatureList request to server to check for Music only virtual folder
    //on error emits the default root id
    static class GetFeatureListOnSubscribe implements Observable.OnSubscribe<String> {
        final AndroidUpnpService upnpService;
        final RemoteService cdService;

        public GetFeatureListOnSubscribe(AndroidUpnpService upnpService, RemoteService cdService) {
            this.upnpService = upnpService;
            this.cdService = cdService;
        }

        @Override
        public void call(final Subscriber<? super String> subscriber) {
            upnpService.getControlPoint()
                    .execute(new GetFeatureList(cdService) {
                        @Override
                        public void received(ActionInvocation actionInvocation, Features features) {
                            Feature feature = features.getFeature(Feature.SEC_BASICVIEW, 1);
                            if (feature != null) {
                                String id = feature.getContainerIdOf(AudioItem.CLASS);
                                if (id != null) {
                                    sendId(id);
                                    return;
                                }
                            }
                            sendId(UpnpCDUris.DEFAULT_ROOT_FOLDER);
                        }

                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                            //Straight up ignore this failure for proprietary feature
                            sendId(UpnpCDUris.DEFAULT_ROOT_FOLDER);
                        }

                        void sendId(String id) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(id);
                                subscriber.onCompleted();
                            }
                        }
                    });
        }
    }

    //performs the browse
    static class BrowseOnSubscribe implements Observable.OnSubscribe<Model> {
        final String mAuthority;
        final AndroidUpnpService upnpService;
        final RemoteService rs;
        final String folderIdentity;
        final BrowseFlag browseFlag;

        public BrowseOnSubscribe(String mAuthority, AndroidUpnpService upnpService,
                                 RemoteService rs, String folderIdentity, BrowseFlag browseFlag) {
            this.mAuthority = mAuthority;
            this.upnpService = upnpService;
            this.rs = rs;
            this.folderIdentity = folderIdentity;
            this.browseFlag = browseFlag;
        }

        @Override
        public void call(final Subscriber<? super Model> subscriber) {
            final Browse browse = new Browse(rs, folderIdentity, browseFlag) {
                @Override
                public void received(ActionInvocation actionInvocation, DIDLContent didl) {
                    if (!subscriber.isUnsubscribed()) {
                        parseDidlContent(didl, actionInvocation, subscriber);
                    }
                }

                @Override
                public void updateStatus(Status status) {
                    //pass
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(new IOException(defaultMsg));
                    }
                }
            };
            upnpService.getControlPoint().execute(browse);
        }

        void parseDidlContent(DIDLContent didlContent, ActionInvocation actionInvocation,
                              Subscriber<? super Model> subscriber) {
            final List<Container> containers = didlContent.getContainers();
            final List<Item> items = didlContent.getItems();
            final List<Model> resources = new ArrayList<>(containers.size() + items.size());

            final String deviceId = rs.getDevice().getIdentity().getUdn().getIdentifierString();

            for (Container c : didlContent.getContainers()) {
                Model b = null;
                if (MusicArtist.CLASS.equals(c)) {
                    b = ModelUtil.parseArtist(mAuthority, deviceId, (MusicArtist) c);
                } else if (MusicAlbum.CLASS.equals(c)) {
                    b = ModelUtil.parseAlbum(mAuthority, deviceId, (MusicAlbum) c);
                } else {
                    b = ModelUtil.parseFolder(mAuthority, deviceId, c);
                }
                if (b != null) {
                    subscriber.onNext(b);
                }
            }

            for (Item item : items) {
                if (MusicTrack.CLASS.equals(item)) {
                    MusicTrack mt = (MusicTrack) item;
                    Track s = ModelUtil.parseSong(mAuthority, deviceId, mt);
                    if (s != null) {
                        subscriber.onNext(s);
                    }
                }
            }

            //TODO handle pagination
            UnsignedIntegerFourBytes numRet = (UnsignedIntegerFourBytes)
                    actionInvocation.getOutput("NumberReturned").getValue();
            UnsignedIntegerFourBytes total = (UnsignedIntegerFourBytes)
                    actionInvocation.getOutput("TotalMatches").getValue();
            // server was unable to compute total matches
            if (numRet.getValue() != 0 && total.getValue() == 0) {
                if (containers.size() != 0 && items.size() != 0) {
                    //TODO
                }
            } else if (numRet.getValue() == 0 && total.getValue() == 720) {
                // no results, total should return an error
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        }
    }

    final Func0<UpnpServiceServiceConnection> mConnectionFactory =
            new Func0<UpnpServiceServiceConnection>() {
                @Override
                public UpnpServiceServiceConnection call() {
                    try {
                        return bindService();
                    } catch (InterruptedException e) {
                        throw OnErrorThrowable.from(e);
                    }
                }
            };

    final Action1<UpnpServiceServiceConnection> mConnectionCloseAction =
            new Action1<UpnpServiceServiceConnection>() {
                @Override
                public void call(UpnpServiceServiceConnection upnpServiceServiceConnection) {
                    upnpServiceServiceConnection.close();
                }
            };

    public final static class UpnpServiceServiceConnection implements Closeable {
        private final Context context;
        private final ServiceConnection serviceConnection;
        private final AndroidUpnpService service;
        private UpnpServiceServiceConnection(
                Context context,
                ServiceConnection serviceConnection,
                AndroidUpnpService service
        ) {
            this.context = new ContextWrapper(context);
            this.serviceConnection = serviceConnection;
            this.service = service;
        }
        @Override public void close() {
            context.unbindService(serviceConnection);
        }
        public AndroidUpnpService getService() {
            return service;
        }
    }

    public UpnpServiceServiceConnection bindService() throws InterruptedException {
        final Context context = getContext();
        ensureNotOnMainThread(context);
        final BlockingQueue<AndroidUpnpService> q = new LinkedBlockingQueue<>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    //Always on space available
                    q.offer((AndroidUpnpService) service);
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };
        //noinspection ConstantConditions
        boolean isBound = context.bindService(new Intent(context, UpnpServiceService.class),
                keyChainServiceConnection,
                Context.BIND_AUTO_CREATE);
        if (!isBound) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new UpnpServiceServiceConnection(context, keyChainServiceConnection, q.take());
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

}
