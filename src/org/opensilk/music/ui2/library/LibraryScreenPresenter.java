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

package org.opensilk.music.ui2.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.StartActivityForResult;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class LibraryScreenPresenter extends ViewPresenter<LibraryScreenView> {

    final LibraryConnection connection;
    final PluginInfo pluginInfo;
    final PluginConfig pluginConfig;
    final LibraryInfo libraryInfo;
    final ArtworkRequestManager requestor;
    final Context appContext;
    final AppPreferences settings;
    final EventBus bus;
    final ActionBarOwner actionBarOwner;
    final MusicServiceConnection musicService;

    final ResultObserver resultObserver;
    final BackgroundWork backgroundWorker;

    final LibraryOverflowHandlers.Bundleables overflowHandler;

    Subscription resultSubscription;
    boolean isloading;
    boolean initialLoad = true;

    boolean progressShowing;

    @Inject
    public LibraryScreenPresenter(LibraryConnection connection,
                                  PluginInfo pluginInfo,
                                  final PluginConfig pluginConfig,
                                  LibraryInfo libraryInfo,
                                  ArtworkRequestManager requestor,
                                  @ForApplication Context appContext,
                                  AppPreferences settings,
                                  @Named("activity") EventBus bus,
                                  ActionBarOwner actionBarOwner,
                                  MusicServiceConnection musicService) {
        this.connection = connection;
        this.pluginInfo = pluginInfo;
        this.pluginConfig = pluginConfig;
        this.libraryInfo = libraryInfo;
        this.requestor = requestor;
        this.appContext = appContext;
        this.settings = settings;
        this.bus = bus;
        this.actionBarOwner = actionBarOwner;
        this.musicService = musicService;

        resultObserver = new ResultObserver();
        backgroundWorker = new BackgroundWork(this);
        overflowHandler = new LibraryOverflowHandlers.Bundleables(this);
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope");
        super.onEnterScope(scope);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad(%s)", libraryInfo);
        super.onLoad(savedInstanceState);
        setupActionBar();
        if (savedInstanceState != null) {
            progressShowing = savedInstanceState.getBoolean("progress_showing", false);
        }
        if (connection.hasCache(libraryInfo)) {
            Timber.v("cacheHit(%s)", libraryInfo);
            LibraryConnection.Result cachedResult = connection.getCache(libraryInfo);
            resultObserver.lastResult = cachedResult;
            initialLoad = true;
            onNewResult(cachedResult);
        } else {
            Timber.v("freshLoad(%s)", libraryInfo);
            loadMore(null);
        }
        if (progressShowing) {
            getView().showProgressDialog();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        outState.putBoolean("progress_showing", progressShowing);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (isSubscribed(resultSubscription)) {
            resultSubscription.unsubscribe();
            resultSubscription = null;
        }
    }

    public void loadMore(Bundle token) {
        if (isloading) return;
        loadMore(token, 0);
    }

    public void loadMore(final Bundle token, long delayMilli) {
        Timber.v("loadMore()");
        LibraryScreenView v = getView();
        if (v != null) {
            if (initialLoad) {
                v.setLoading(true);
            } else {
                v.setMoreLoading(true);
            }
        }
        if (isSubscribed(resultSubscription)) resultSubscription.unsubscribe();
        resultSubscription = Observable.timer(delayMilli, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<Long, Observable<LibraryConnection.Result>>() {
                    @Override
                    public Observable<LibraryConnection.Result> call(Long aLong) {
                        return connection.browse(pluginInfo, libraryInfo, token);
                    }
                })
                .subscribe(resultObserver);
    }

    public void onNewResult(LibraryConnection.Result result) {
        isloading = false;
        LibraryScreenView v = getView();
        if (v == null) return;
        if (initialLoad) {
            initialLoad = false;
            if (result.items.isEmpty()) {
                v.setListEmpty(true, true);
            } else {
                v.setListShown(true, true);
            }
        } else {
            v.setMoreLoading(false);
        }
        v.adapter.onNewResult(result);
    }

    public void onItemClicked(Context context, Bundleable item) {
        if (item instanceof Song) {
            final Song song = (Song) item;
            musicService.playAllSongs(new Func0<Song[]>() {
                @Override
                public Song[] call() {
                    return new Song[]{song};
                }
            }, 0, false);
            return;
        }
        String identity = item.getIdentity();
        String name = item.getName();
        if (TextUtils.isEmpty(identity)) return; //Shouldnt happen
        LibraryInfo newInfo = libraryInfo.buildUpon(identity, name);
        AppFlow.get(context).goTo(new LibraryScreen(pluginInfo, pluginConfig, newInfo));
    }

    public void startWork(OverflowAction action, Bundleable item) {
        progressShowing = true;
        if (getView() != null) getView().showProgressDialog();
        backgroundWorker.startWork(action, item);
    }

    public void cancelWork() {
        backgroundWorker.cancelWork();
    }

    public void dismissProgressDialog() {
        progressShowing = false;
        if (getView() != null) getView().dismissProgressDialog();
    }

    void setupActionBar() {
        actionBarOwner.setConfig(
                new ActionBarOwner.Config.Builder()
                        .setTitle(pluginInfo.title)
                        .setSubtitle(
                                !TextUtils.isEmpty(libraryInfo.folderName)
                                        ? libraryInfo.folderName : libraryInfo.libraryName
                        )
                        .setMenuConfig(createMenuConfig())
                        .build()
        );
    }

    ActionBarOwner.MenuConfig createMenuConfig() {
        ActionBarOwner.MenuConfig.Builder builder = new ActionBarOwner.MenuConfig.Builder();

        // Common items
        builder.withMenus(LibraryOverflowHandlers.Bundleables.MENUS_COLLECTION);

        // device selection
        String selectName = pluginConfig.getMeta(PluginConfig.META_MENU_NAME_PICKER);
        if (!TextUtils.isEmpty(selectName)) {
            builder.withMenus(new ActionBarOwner.CustomMenuItem(
                    0, R.id.menu_change_source, 99, selectName, -1
            ));
        } else {
            builder.withMenus(R.menu.library_change_source);
        }

        // library settings
        if (pluginConfig.hasAbility(PluginConfig.SETTINGS)) {
            String settingsName = pluginConfig.getMeta(PluginConfig.META_MENU_NAME_SETTINGS);
            if (!TextUtils.isEmpty(settingsName)) {
                builder.withMenus(new ActionBarOwner.CustomMenuItem(
                        0, R.id.menu_library_settings, 100, settingsName, -1
                ));
            } else {
                builder.withMenus(R.menu.library_settings);
            }
        }

        //set handler
        builder.setActionHandler(createMenuActionHandler());

        return builder.build();
    }

    Func1<Integer, Boolean> createMenuActionHandler() {
        return new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(final Integer integer) {
                switch (integer) {
                    case R.id.menu_change_source:
                        settings.removeDefaultLibraryInfo(pluginInfo);
                        if (getView() != null) {
                            AppFlow.get(getView().getContext()).resetTo(new PluginScreen(pluginInfo));
                        }
                        return true;
                    case R.id.menu_library_settings:
                        Intent intent = new Intent()
                                .setComponent(pluginConfig.<ComponentName>getMeta(PluginConfig.META_SETTINGS_COMPONENT))
                                .putExtra(OrpheusApi.EXTRA_LIBRARY_ID, libraryInfo.libraryId)
                                .putExtra(OrpheusApi.EXTRA_LIBRARY_INFO, libraryInfo);
                        bus.post(new StartActivityForResult(intent,
                                StartActivityForResult.PLUGIN_REQUEST_SETTINGS));
                        return true;
                    default:
                        try {
                            OverflowAction action = OverflowAction.valueOf(integer);
                            return overflowHandler.handleClick(action, new Bundleable() {
                                @Override
                                public Bundle toBundle() {
                                    return null;
                                }

                                @Override
                                public String getIdentity() {
                                    return libraryInfo.folderId;
                                }

                                @Override
                                public String getName() {
                                    return libraryInfo.folderName;
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                }
            }
        };
    }

    class ResultObserver implements Observer<LibraryConnection.Result> {
        final int RETRY_LIMIT = 5;

        LibraryConnection.Result lastResult;
        int retryCount = 0;

        @Override
        public void onCompleted() {
            retryCount = 0;
        }

        @Override
        public void onError(Throwable e) {
            Timber.w(e, "ResultObserver.OnError()");
            if (retryCount++ >= RETRY_LIMIT) {
                bus.post(new MakeToast(R.string.err_retrieving_items));
                if (getView() != null) {
                    getView().setListEmpty(true, true);
                }
                return;
            }
            Bundle token = lastResult != null ? lastResult.token : null;
            int backoff = (int) ((Math.pow(2, retryCount) + Math.random()) * 1000);
            Timber.d("retry=%d backoff=%d", retryCount, backoff);
            if (e instanceof RemoteException) {
                connection.connectionManager.onException(pluginInfo.componentName);
                loadMore(token, backoff);
            } else if (e instanceof ParcelableException) {
                int code = ((ParcelableException) e).getCode();
                switch (code) {
                    case ParcelableException.NETWORK:
                        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo info = cm.getActiveNetworkInfo();
                        if (info != null && info.isConnectedOrConnecting()) {
                            loadMore(token, backoff);
                        } else {
                            bus.post(new MakeToast(R.string.err_offline));
                        }
                        break;
                    case ParcelableException.AUTH_FAILURE:
                        settings.removeDefaultLibraryInfo(pluginInfo);
                        bus.post(new MakeToast(R.string.err_authentication));
                        if (getView() != null) {
                            AppFlow.get(getView().getContext()).resetTo(new PluginScreen(pluginInfo));
                        }
                        break;
                    case ParcelableException.RETRY:
                        loadMore(token, backoff);
                        break;
                    case ParcelableException.UNKNOWN:
                        connection.connectionManager.onException(pluginInfo.componentName);
                        loadMore(token, backoff);
                        break;
                    default:
                        break;
                }
                return;
            }
            Timber.e(e, "ResultObserver: Unhandled exception");
        }

        @Override
        public void onNext(LibraryConnection.Result result) {
            Timber.v("ResultObserver.onNext() called from:" + Thread.currentThread().getName());
            Timber.v("ResultObserver %s, %s", result.items, result.token);
            lastResult = result;
            onNewResult(result);
        }
    }

}
