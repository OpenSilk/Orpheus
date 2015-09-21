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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
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
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.upnp.R;
import org.opensilk.music.library.upnp.UpnpLibraryComponent;
import org.opensilk.music.library.upnp.UpnpServiceService;
import org.opensilk.music.library.upnp.util.ModelUtil;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.upnp.contentdirectory.Feature;
import org.opensilk.upnp.contentdirectory.Features;
import org.opensilk.upnp.contentdirectory.callback.GetFeatureList;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 9/17/15.
 */
public class UpnpCDProvider extends LibraryProvider {

    @Inject @Named("UpnpLibraryBaseAuthority") String mBaseAuthority;
    UriMatcher mMatcher;

    @Override
    public boolean onCreate() {
        UpnpLibraryComponent app = DaggerService.getDaggerComponent(getContext());
        UpnpCDComponent.FACTORY.call(app).inject(this);
        super.onCreate();
        mMatcher = UpnpCDUris.makeMatcher(mAuthority);
        return true;
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLabel(getContext().getResources().getString(R.string.app_name))
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return mBaseAuthority;
    }

    @Override
    protected void listObjsInternal(Uri uri, IBinder binder, Bundle args) {
        switch (mMatcher.match(uri)) {
            case UpnpCDUris.M_OBJECT: {

                final String device = uri.getPathSegments().get(0);
                final String folder = uri.getLastPathSegment();

                Observable.create(
                        new Observable.OnSubscribe<List<Bundleable>>() {
                            @Override
                            public void call(Subscriber<? super List<Bundleable>> subscriber) {
                                browseFolders(device, folder, subscriber);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .subscribe(new BundleableSubscriber<Bundleable>(binder));

                break;
            }
            default: {
                super.listObjsInternal(uri, binder, args);
            }
        }
    }

    @Override
    protected void getObjInternal(Uri uri, IBinder binder, Bundle args) {
        super.getObjInternal(uri, binder, args);
    }

    @Override
    protected void scanObjsInternal(Uri uri, IBinder binder, Bundle args) {
        args.putBoolean("upnp.scan", true);
        listObjsInternal(uri, binder, args);
    }

    @Override
    protected void listRootsInternal(Uri uri, IBinder binder, Bundle args) {

        final BundleableSubscriber<Folder> subscriber
                = new BundleableSubscriber<>(binder);

        Observable.create(new Observable.OnSubscribe<Folder>() {
            @Override
            public void call(final Subscriber<? super Folder> subscriber) {
                final UpnpServiceServiceConnection connection;
                try {
                    connection = bindService(getContext());
                } catch (InterruptedException e) {
                    subscriber.onError(e);
                    return;
                }

                Collection<Device> devices = connection.getService().getRegistry().getDevices();
                if (devices.size() > 0) {
                    for (Device device : devices) {
                        final String id = device.getIdentity().getUdn().getIdentifierString();
                        final String label = !StringUtils.isEmpty(device.getDetails().getFriendlyName()) ?
                                device.getDetails().getFriendlyName() : device.getDisplayString();
                        Folder folder = Folder.builder()
                                .setUri(UpnpCDUris.makeUri(mAuthority, id, "0"))
                                .setParentUri(LibraryUris.rootUri(mAuthority))
                                .setName(label)
                                .build();
                        subscriber.onNext(folder);
                    }
                    subscriber.onCompleted();
                    connection.close();
                    return;
                }

                final RegistryListener registryListener = new DefaultRegistryListener() {
                    @Override
                    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                        try {
                            final String id = device.getIdentity().getUdn().getIdentifierString();
                            final String label = !StringUtils.isEmpty(device.getDetails().getFriendlyName()) ?
                                    device.getDetails().getFriendlyName() : device.getDisplayString();
                            Folder folder = Folder.builder()
                                    .setUri(UpnpCDUris.makeUri(mAuthority, id, "0"))
                                    .setParentUri(LibraryUris.rootUri(mAuthority))
                                    .setName(label)
                                    .build();

                            subscriber.onNext(folder);
                            subscriber.onCompleted();
                        } finally {
                            connection.getService().getRegistry().removeListener(this);
                            connection.close();
                        }
                    }
                };

                connection.getService().getRegistry().addListener(registryListener);
            }
        }).toList().subscribe(subscriber);

    }

    protected void browseFolders(
            @NonNull final String libraryIdentity,
            @Nullable String folderIdentity,
            Subscriber<? super List<Bundleable>> subscription
    ) {
        final UpnpServiceServiceConnection connection;
        try {
            connection = bindService(getContext());
        } catch (InterruptedException e) {
            subscription.onError(e);
            return;
        }
        try {
            final AndroidUpnpService upnpService = connection.getService();
            final RemoteService rs = acquireContentDirectoryService(upnpService, libraryIdentity);
            if (rs == null) {
                subscription.onError(new NullPointerException("Unable to obtain service id=" + libraryIdentity));
                connection.close();
                return;
            }
            List<Bundleable> lst;
            // Requested root folder
            if (StringUtils.isEmpty(folderIdentity)) {
                // lets see if there is a music only virtual folder
                lst = requestFeaturesSync(upnpService, rs, new Command() {
                    @Override
                    public List<Bundleable> call(AndroidUpnpService androidUpnpService, Service service, String s) {
                        return doBrowseSync(androidUpnpService, service, s);
                    }
                });

            } else {
                lst = doBrowseSync(upnpService, rs, folderIdentity);
            }
            subscription.onNext(lst);
            subscription.onCompleted();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private RemoteService acquireContentDirectoryService(
            final AndroidUpnpService upnpService,
            final String deviceIdentity
    ) {
        RemoteDevice rd = upnpService.getRegistry().getRemoteDevice(UDN.valueOf(deviceIdentity), false);
        if (rd != null) {
            RemoteService rs = rd.findService(new UDAServiceType("ContentDirectory", 1));
            if (rs != null) {
                return rs;
            }
        }
        return null;
    }

    static abstract class Command implements Func3 <AndroidUpnpService, Service, String, List<Bundleable>> {

    }

    private List<Bundleable> requestFeaturesSync(
            final AndroidUpnpService upnpService,
            final RemoteService service,
            final Command command
    ) {
        GetFeatureListSync req = new GetFeatureListSync(service);
        req.run(); //Long running;
        if (req.features != null) {
            Feature feature = req.features.getFeature(Feature.SEC_BASICVIEW, 1);
            if (feature != null) {
                String id = feature.getContainerIdOf(AudioItem.CLASS);
                if (id != null) {
                    return command.call(upnpService, service, null);
                }
            }
        }
        return command.call(upnpService, service, UpnpCDUris.DEFAULT_ROOT_FOLDER);
    }

    static class GetFeatureListSync extends GetFeatureList {
        public GetFeatureListSync(Service service) {
            super(service);
        }

        ActionInvocation actionInvocation;
        Features features;
        UpnpResponse upnpResponse;
        String errorMsg;

        @Override
        public void received(ActionInvocation actionInvocation, Features features) {
            this.actionInvocation = actionInvocation;
            this.features = features;
        }

        @Override
        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            this.actionInvocation = invocation;
            this.upnpResponse = operation;
            this.errorMsg = defaultMsg;
        }
    }

    private List<Bundleable> doBrowseSync(
            final AndroidUpnpService upnpService,
            final Service rs,
            final String folderIdentity
    ) {

        final BrowseSync browse = new BrowseSync(rs, folderIdentity, BrowseFlag.DIRECT_CHILDREN);
        browse.setControlPoint(upnpService.getControlPoint());
        browse.run();//Long running op
        if (browse.status == null || browse.status != Browse.Status.OK) {
            return Collections.emptyList();
        }

        final List<Container> containers = browse.didlContent.getContainers();
        final List<Item> items = browse.didlContent.getItems();
        final List<Bundleable> resources = new ArrayList<>(containers.size() + items.size());

        final String deviceId = rs.getDevice().getIdentity().getUdn().getIdentifierString();

        for (Container c : containers) {
            Bundleable b = null;
            if (MusicArtist.CLASS.equals(c)) {
                b = ModelUtil.parseArtist(mAuthority, deviceId, (MusicArtist) c);
            } else if (MusicAlbum.CLASS.equals(c)) {
                b = ModelUtil.parseAlbum(mAuthority, deviceId, (MusicAlbum) c);
            } else {
                b = ModelUtil.parseFolder(mAuthority, deviceId, c);
            }
            if (b != null) {
                resources.add(b);
            }
        }

        for (Item item : items) {
            if (MusicTrack.CLASS.equals(item)) {
                MusicTrack mt = (MusicTrack) item;
                Track s = ModelUtil.parseSong(mAuthority, deviceId, mt);
                if (s != null) {
                    resources.add(s);
                }
            }
        }

        UnsignedIntegerFourBytes numRet = (UnsignedIntegerFourBytes)
                browse.actionInvocation.getOutput("NumberReturned").getValue();
        UnsignedIntegerFourBytes total = (UnsignedIntegerFourBytes)
                browse.actionInvocation.getOutput("TotalMatches").getValue();
        // server was unable to compute total matches
        if (numRet.getValue() != 0 && total.getValue() == 0) {
            if (containers.size() != 0 && items.size() != 0) {
                //TODO
            }
        } else if (numRet.getValue() == 0 && total.getValue() == 720) {
            // no results, total should return an error
        }

        return resources;
    }

    static class BrowseSync extends Browse {
        BrowseSync(Service rs, String folderId, BrowseFlag flag) {
            super(rs, folderId, flag);
        }

        ActionInvocation actionInvocation;
        DIDLContent didlContent;
        Status status;
        UpnpResponse upnpResponse;
        String errorMsg;

        @Override public void received(ActionInvocation actionInvocation, DIDLContent didlContent) {
            this.actionInvocation = actionInvocation;
            this.didlContent = didlContent;
        }

        @Override public void updateStatus(Status status) {
            this.status = status;
        }

        @Override public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
            this.actionInvocation = actionInvocation;
            this.upnpResponse = upnpResponse;
            this.errorMsg = s;
        }
    }

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

    public static UpnpServiceServiceConnection bindService(Context context) throws InterruptedException {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        ensureNotOnMainThread(context);
        final BlockingQueue<AndroidUpnpService> q = new LinkedBlockingQueue<AndroidUpnpService>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    try {
                        q.put((AndroidUpnpService) service);
                    } catch (InterruptedException e) {
                        // will never happen, since the queue starts with one available slot
                    }
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };
        Intent intent = new Intent(context, UpnpServiceService.class);
        boolean isBound = context.bindService(intent,
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
