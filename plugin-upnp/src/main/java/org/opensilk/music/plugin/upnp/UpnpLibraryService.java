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

package org.opensilk.music.plugin.upnp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Search;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.RemoteLibraryService;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.plugin.common.LibraryPreferences;
import org.opensilk.music.plugin.upnp.ui.LibraryPickerActivity;
import org.opensilk.music.plugin.upnp.ui.SettingsActivity;
import org.opensilk.music.plugin.upnp.util.Helpers;
import org.opensilk.upnp.contentdirectory.Feature;
import org.opensilk.upnp.contentdirectory.Features;
import org.opensilk.upnp.contentdirectory.callback.GetFeatureList;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.AsyncSubject;

import static org.opensilk.music.api.exception.ParcelableException.NETWORK;
import static org.opensilk.music.api.exception.ParcelableException.RETRY;

/**
 * Created by drew on 6/8/14.
 */
public class UpnpLibraryService extends RemoteLibraryService {

    public static final String DEFAULT_ROOT_FOLDER = "0";

    @Inject LibraryPreferences mLibraryPrefs;

    Token mUpnpServiceToken;

    @Override
    public void onCreate() {
        super.onCreate();
        ((DaggerInjector) getApplication()).getObjectGraph().inject(this);
        bindUpnpService();
    }

    @Override
    public void onDestroy() {
        unbindUpnpService();
        super.onDestroy();
    }

    synchronized Observable<AndroidUpnpService> bindUpnpService() {
        mUpnpServiceToken = new Token(AsyncSubject.<AndroidUpnpService>create());
        bindService(new Intent(this, UpnpServiceService.class), mUpnpServiceToken, BIND_AUTO_CREATE);
        return mUpnpServiceToken.subject.asObservable();
    }

    synchronized void unbindUpnpService() {
        if (mUpnpServiceToken != null) {
            unbindService(mUpnpServiceToken);
            mUpnpServiceToken = null;
        }
    }

    /*
     * Abstract methods
     */

    @Override
    protected PluginConfig getConfig() {
        return new PluginConfig.Builder()
                .addAbility(PluginConfig.SEARCHABLE)
                .setPickerComponent(new ComponentName(this, LibraryPickerActivity.class),
                        getResources().getString(R.string.menu_change_source))
                .setSettingsComponent(new ComponentName(this, SettingsActivity.class),
                        getResources().getString(R.string.menu_library_settings))
                .build();
    }

    @Override
    protected synchronized void pause() {
        if (mUpnpServiceToken != null) {
            mUpnpServiceToken.subject.asObservable().subscribe(new Action1<AndroidUpnpService>() {
                @Override
                public void call(AndroidUpnpService androidUpnpService) {
                    if (!androidUpnpService.getRegistry().isPaused()) {
                        androidUpnpService.getRegistry().pause();
                    }
                }
            });
        }
    }

    @Override
    protected synchronized void resume() {
        if (mUpnpServiceToken != null) {
            mUpnpServiceToken.subject.asObservable().subscribe(new Action1<AndroidUpnpService>() {
                @Override
                public void call(AndroidUpnpService androidUpnpService) {
                    if (androidUpnpService.getRegistry().isPaused()) {
                        androidUpnpService.getRegistry().resume();
                    }
                }
            });
        }
    }

    @Override
    @DebugLog
    protected void browseFolders(@NonNull final String libraryIdentity,
                                          final String folderIdentity,
                                          final int maxResults,
                                          final Bundle paginationBundle,
                                 @NonNull final Result callback) {
        bindUpnpService().subscribe(new Action1<AndroidUpnpService>() {
            @Override
            public void call(AndroidUpnpService upnpService) {
                RemoteService rs = acquireContentDirectoryService(upnpService, libraryIdentity);
                if (rs != null) {
                    // Requested root folder
                    if (TextUtils.isEmpty(folderIdentity)) {
                        String rootFolder = mLibraryPrefs.getRootFolder(libraryIdentity);
                        if (!TextUtils.isEmpty(rootFolder)) {
                            // use preferred root folder
                            doBrowse(upnpService, rs, rootFolder, maxResults, paginationBundle, callback, false);
                        } else {
                            // lets see if there is a music only virtual folder
                            requestFeatures(upnpService, rs, new BrowseCommand(upnpService, rs, maxResults, paginationBundle, callback));
                        }
                    } else {
                        doBrowse(upnpService, rs, folderIdentity, maxResults, paginationBundle, callback, false);
                    }
                } else {
                    try {
                        callback.onError(new ParcelableException(RETRY,
                                new NullPointerException("Unable to obtain service id=" + libraryIdentity)));
                    } catch (RemoteException ignored) {}
                }
            }
        });
    }

    @Override
    protected void listSongsInFolder(@NonNull final String libraryIdentity,
                                              final String folderIdentity,
                                              final int maxResults,
                                              final Bundle paginationBundle,
                                     @NonNull final Result callback) {
        bindUpnpService().subscribe(new Action1<AndroidUpnpService>() {
            @Override
            public void call(AndroidUpnpService upnpService) {
                RemoteService rs = acquireContentDirectoryService(upnpService, libraryIdentity);
                if (rs != null) {
                    doBrowse(upnpService, rs, folderIdentity, maxResults, paginationBundle, callback, true);
                } else {
                    try {
                        callback.onError(new ParcelableException(RETRY,
                                new NullPointerException("Unable to obtain service id=" + libraryIdentity)));
                    } catch (RemoteException ignored) {}
                }
            }
        });
    }

    @Override
    protected void search(@NonNull final String libraryIdentity,
                          @NonNull final String query,
                                   final int maxResults,
                                   final Bundle paginationBundle,
                          @NonNull final Result callback) {
        bindUpnpService().subscribe(new Action1<AndroidUpnpService>() {
            @Override
            public void call(AndroidUpnpService upnpService) {
                RemoteService rs = acquireContentDirectoryService(upnpService, libraryIdentity);
                if (rs != null) {
                    String searchFolder = mLibraryPrefs.getSearchFolder(libraryIdentity);
                    if (!TextUtils.isEmpty(searchFolder)) {
                        // use preferred search folder
                        doSearch(upnpService, rs, searchFolder, query, maxResults, paginationBundle, callback);
                    } else {
                        // lets see if there is a music only virtual folder
                        requestFeatures(upnpService, rs, new SearchCommand(upnpService, rs, query, maxResults, paginationBundle, callback));
                    }
                } else {
                    try {
                        callback.onError(new ParcelableException(RETRY,
                                new NullPointerException("Unable to obtain service id=" + libraryIdentity)));
                    } catch (RemoteException ignored) {}
                }
            }
        });
    }

    /*
     *
     */

    private RemoteService acquireContentDirectoryService(final AndroidUpnpService upnpService,
                                                         final String deviceIdentity) {
        RemoteDevice rd = upnpService.getRegistry().getRemoteDevice(UDN.valueOf(deviceIdentity), false);
        if (rd != null) {
            RemoteService rs = rd.findService(new UDAServiceType("ContentDirectory", 1));
            if (rs != null) {
                return rs;
            }
        }
        return null;
    }

    private void requestFeatures(final AndroidUpnpService upnpService,
                                 final RemoteService service,
                                 final Command command) {
        GetFeatureList req = new GetFeatureList(service) {
            @Override
            public void received(ActionInvocation actionInvocation, Features features) {
                command.execute(features);
            }

            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                command.execute(null);
            }
        };
        upnpService.getControlPoint().execute(req);
    }

    @DebugLog
    private void doBrowse(final AndroidUpnpService upnpService,
                          final RemoteService rs,
                          final String folderIdentity,
                          final int maxResults,
                          final Bundle paginationBundle,
                          final Result callback,
                          final boolean songsOnly) {
        final int start = paginationBundle != null ? paginationBundle.getInt("start") : 0;
        final Browse browse = new Browse(rs, folderIdentity, BrowseFlag.DIRECT_CHILDREN, Browse.CAPS_WILDCARD, start, (long)maxResults) {
            @Override
            @DebugLog
            public void received(ActionInvocation actionInvocation, DIDLContent didlContent) {

                final List<Container> containers = didlContent.getContainers();
                final List<Item> items = didlContent.getItems();
                final List<Bundle> resources = new ArrayList<>(containers.size() + items.size());

                if (!songsOnly) {
                    for (Container c : containers) {
                        Bundle b = null;
                        if (MusicArtist.CLASS.equals(c)) {
                            Artist a = Helpers.parseArtist((MusicArtist) c);
                            if (a != null) {
                                b = a.toBundle();
                            }
                        } else if (MusicAlbum.CLASS.equals(c)) {
                            Album a = Helpers.parseAlbum((MusicAlbum)c);
                            if (a != null) {
                                b = a.toBundle();
                            }
                        } else {
                            Folder f = Helpers.parseFolder(c);
                            if (f != null) {
                                b = f.toBundle();
                            }
                        }
                        if (b != null) {
                            resources.add(b);
                        }
                    }
                }

                for (Item item : items) {
                    if (MusicTrack.CLASS.equals(item)) {
                        MusicTrack mt = (MusicTrack) item;
                        Song s = Helpers.parseSong(mt);
                        if (s != null) {
                            resources.add(s.toBundle());
                        }
                    }
                }

                UnsignedIntegerFourBytes numRet = (UnsignedIntegerFourBytes) actionInvocation.getOutput("NumberReturned").getValue();
                UnsignedIntegerFourBytes total = (UnsignedIntegerFourBytes) actionInvocation.getOutput("TotalMatches").getValue();
                final Bundle b;
                // server was unable to compute total matches
                if (numRet.getValue() != 0 && total.getValue() == 0) {
                    // this isnt exactly right, it will cause an extra call
                    // but i dont know of a more specific manner to determine
                    // end of results
                    if (containers.size() == 0 && items.size() == 0) {
                        b = null;
                    } else {
                        b = new Bundle(1);
                        b.putInt("start", start+maxResults);
                    }
                    // no results, total should return an error
                } else if (numRet.getValue() == 0 && total.getValue() == 720) {
                    b = null;
                } else {
                    int nextStart = start+maxResults;
                    if (nextStart < total.getValue()) {
                        b = new Bundle(1);
                        b.putInt("start", nextStart);
                    } else {
                        b = null;
                    }
                }

                try {
                    callback.onNext(resources, b);
                } catch (RemoteException ignored) { }
            }

            @Override
            public void updateStatus(Status status) {

            }

            @Override
            @DebugLog
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                try {
                    callback.onError(new ParcelableException(NETWORK, new Exception(s)));
                } catch (RemoteException ignored) { }
            }
        };
        upnpService.getControlPoint().execute(browse);
    }

    private void doSearch(final AndroidUpnpService upnpService,
                          final RemoteService rs,
                          final String folderIdentity,
                          final String query,
                          final int maxResults,
                          final Bundle paginationBundle,
                          final Result callback) {
        final int start = paginationBundle != null ? paginationBundle.getInt("start") : 0;
        final Search search = new Search(rs, folderIdentity,
                "dc:title contains \"" + query + "\" or upnp:artist contains \""
                        + query + "\" or upnp:album contains \"" + query + "\""
                        + " or upnp:genre contains \"" + query + "\"",
                "*", start, (long)maxResults) {
            @Override
            public void received(ActionInvocation actionInvocation, DIDLContent didlContent) {
                final List<Container> containers = didlContent.getContainers();
                final List<Item> items = didlContent.getItems();
                final List<Bundle> resources = new ArrayList<>(containers.size() + items.size());

                for (Container c : containers) {
                    Bundle b = null;
                    if (MusicArtist.CLASS.equals(c)) {
                        Artist a = Helpers.parseArtist((MusicArtist) c);
                        if (a != null) {
                            b = a.toBundle();
                        }
                    } else if (MusicAlbum.CLASS.equals(c)) {
                        Album a = Helpers.parseAlbum((MusicAlbum)c);
                        if (a != null) {
                            b = a.toBundle();
                        }
                    } else {
                        Folder f = Helpers.parseFolder(c);
                        if (f != null) {
                            b = f.toBundle();
                        }
                    }
                    if (b != null) {
                        resources.add(b);
                    }
                }

                for (Item item : items) {
                    if (MusicTrack.CLASS.equals(item)) {
                        MusicTrack mt = (MusicTrack) item;
                        Song s = Helpers.parseSong(mt);
                        if (s != null) {
                            resources.add(s.toBundle());
                        }
                    }
                }

                UnsignedIntegerFourBytes numRet = (UnsignedIntegerFourBytes) actionInvocation.getOutput("NumberReturned").getValue();
                UnsignedIntegerFourBytes total = (UnsignedIntegerFourBytes) actionInvocation.getOutput("TotalMatches").getValue();
                final Bundle b;
                // server was unable to compute total matches
                if (numRet.getValue() != 0 && total.getValue() == 0) {
                    // this isnt exactly right, it will cause an extra call
                    // but i dont know of a more specific manner to determine
                    // end of results
                    if (containers.size() == 0 && items.size() == 0) {
                        b = null;
                    } else {
                        b = new Bundle(1);
                        b.putInt("start", start+maxResults);
                    }
                    // no results, total should return an error
                } else if (numRet.getValue() == 0 && total.getValue() == 720) {
                    b = null;
                } else {
                    int nextStart = start+maxResults;
                    if (nextStart < total.getValue()) {
                        b = new Bundle(1);
                        b.putInt("start", nextStart);
                    } else {
                        b = null;
                    }
                }

                try {
                    callback.onNext(resources, b);
                } catch (RemoteException ignored) { }
            }

            @Override
            public void updateStatus(Status status) {

            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String s) {
                try {
                    callback.onError(new ParcelableException(NETWORK, new Exception(s)));
                } catch (RemoteException ignored) { }
            }
        };
        upnpService.getControlPoint().execute(search);
    }

    /**
     *
     */
    final class Token implements ServiceConnection {
        final AsyncSubject<AndroidUpnpService> subject;

        private Token(AsyncSubject<AndroidUpnpService> subject) {
            this.subject = subject;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AndroidUpnpService upnpService = (AndroidUpnpService) service;
            if (upnpService != null) {
                upnpService.getControlPoint().search();
            }
            subject.onNext(upnpService);
            subject.onCompleted();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindUpnpService();
        }
    }

    /**
     *
     */
    interface Command {
        void execute(Object data);
    }

    /**
     *
     */
    abstract class UpnpCommand implements Command {
        final AndroidUpnpService upnpService;
        final RemoteService service;
        final int maxResults;
        final Bundle paginationBudle;
        final Result callback;

        UpnpCommand(AndroidUpnpService upnpService,
                    RemoteService service,
                    int maxResults,
                    Bundle paginationBundle,
                    Result callback) {
            this.upnpService = upnpService;
            this.service = service;
            this.maxResults = maxResults;
            this.paginationBudle = paginationBundle;
            this.callback = callback;
        }

        @Override
        public void execute(Object data) {
            String fId = null;
            if (data != null) {
                Features features = (Features) data;
                Feature feature = features.getFeature(Feature.SEC_BASICVIEW, 1);
                if (feature != null) {
                    String id = feature.getContainerIdOf(AudioItem.CLASS);
                    if (id != null) {
                        fId = id;
                    }
                }
            }
            if (fId == null) {
                fId = DEFAULT_ROOT_FOLDER;
            }
            doExecute(fId);
        }

        abstract void doExecute(String folderIdentity);
    }

    /**
     *
     */
    class BrowseCommand extends UpnpCommand {

        BrowseCommand(AndroidUpnpService upnpService,
                      RemoteService service,
                      int maxResults,
                      Bundle paginationBundle,
                      Result callback) {
            super(upnpService, service, maxResults, paginationBundle, callback);
        }

        @Override
        void doExecute(String folderIdentity) {
            doBrowse(upnpService, service, folderIdentity, maxResults, paginationBudle, callback, false);
        }

    }

    /**
     *
     */
    class SearchCommand extends UpnpCommand {
        final String query;

        SearchCommand(AndroidUpnpService upnpService,
                      RemoteService service,
                      String query,
                      int maxResults,
                      Bundle paginationBundle,
                      Result callback) {
            super(upnpService, service, maxResults, paginationBundle, callback);
            this.query = query;
        }

        @Override
        void doExecute(String folderIdentity) {
            doSearch(upnpService, service, folderIdentity, query, maxResults, paginationBudle, callback);
        }
    }

}
