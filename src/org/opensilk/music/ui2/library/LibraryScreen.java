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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.exception.ParcelableException;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.StartActivityForResult;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import flow.Layout;
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
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.library)
@WithModule(LibraryScreen.Module.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.slide_out_right, R.anim.grow_fade_in }
)
public class LibraryScreen extends Screen {

    final PluginInfo pluginInfo;
    final PluginConfig pluginConfig;
    final LibraryInfo libraryInfo;

    public LibraryScreen(PluginInfo pluginInfo, PluginConfig pluginConfig, LibraryInfo libraryInfo) {
        this.pluginInfo = pluginInfo;
        this.pluginConfig = pluginConfig;
        this.libraryInfo = libraryInfo;
    }

    @Override
    public String getName() {
        return super.getName() + libraryInfo.toString();
    }

    @dagger.Module(
            addsTo = LauncherActivity.Module.class,
            injects = LibraryView.class,
            library = true
    )
    public static class Module {

        final LibraryScreen screen;

        public Module(LibraryScreen screen) {
            this.screen = screen;
        }

        @Provides
        public PluginInfo providePluginInfo() {
            return screen.pluginInfo;
        }

        @Provides
        public LibraryInfo provideLibraryInfo() {
            return screen.libraryInfo;
        }

        @Provides
        public PluginConfig provideLibraryConfig() {
            return screen.pluginConfig;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<LibraryView> {

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
        public Presenter(LibraryConnection connection,
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
            LibraryView v = getView();
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
            LibraryView v = getView();
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
                        return new Song[] {song};
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
                                    @Override public Bundle toBundle() { return null; }
                                    @Override public String getIdentity() { return libraryInfo.folderId; }
                                    @Override public String getName() { return libraryInfo.folderName; }
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

}
