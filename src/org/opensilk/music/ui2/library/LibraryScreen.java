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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
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
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import rx.functions.Action1;
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
    final LibraryInfo libraryInfo;

    public LibraryScreen(PluginInfo pluginInfo, LibraryInfo libraryInfo) {
        this.pluginInfo = pluginInfo;
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

    }

    @Singleton
    public static class Presenter extends ViewPresenter<LibraryView> {

        final LibraryConnection connection;
        final PluginInfo pluginInfo;
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
                         LibraryInfo libraryInfo,
                         ArtworkRequestManager requestor,
                         @ForApplication Context appContext,
                         AppPreferences settings,
                         @Named("activity") EventBus bus,
                         ActionBarOwner actionBarOwner,
                         MusicServiceConnection musicService) {
            this.connection = connection;
            this.pluginInfo = pluginInfo;
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
                bus.post(new MakeToast(R.string.err_unimplemented));
                return;
            }
            String identity = item.getIdentity();
            String name = item.getName();
            if (TextUtils.isEmpty(identity)) return; //Shouldnt happen
            LibraryInfo newInfo = libraryInfo.buildUpon(identity, name);
            AppFlow.get(context).goTo(new LibraryScreen(pluginInfo, newInfo));
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
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                    .setTitle(pluginInfo.title)
                    .setSubtitle(libraryInfo.libraryName)
                    .build());
            connection.getCapabilities(pluginInfo).subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer capabilities) {
                    actionBarOwner.setConfig(actionBarOwner.getConfig().buildUpon()
                            .withMenuConfig(createMenuConfig(capabilities)).build());
                }
            });
        }

        ActionBarOwner.MenuConfig createMenuConfig(int capabilities) {
            List<Integer> menus = new ArrayList<>();
            List<ActionBarOwner.CustomMenuItem> customMenus = new ArrayList<>();

            // Common items
            for (int ii : LibraryOverflowHandlers.Bundleables.MENUS_COLLECTION) {
                menus.add(ii);
            }

            // search
            if (hasAbility(capabilities, OrpheusApi.Ability.SEARCH)) {
                menus.add(R.menu.search);
            }

            Resources res = null;
            try {
                res = appContext.getPackageManager()
                        .getResourcesForApplication(pluginInfo.componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {}

            if (res != null) {
                // device selection
                try {
                    String change = res.getString(res.getIdentifier("menu_change_source",
                            "string", pluginInfo.componentName.getPackageName()));
                    customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_change_source, change));
                } catch (Resources.NotFoundException ignored) {
                    menus.add(R.menu.change_source);
                }
                // library settings
                if (hasAbility(capabilities, OrpheusApi.Ability.SETTINGS)) {
                    try {
                        String settings = res.getString(res.getIdentifier("menu_library_settings",
                                "string", pluginInfo.componentName.getPackageName()));
                        customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_library_settings, settings));
                    } catch (Resources.NotFoundException ignored) {
                        menus.add(R.menu.library_settings);
                    }
                }
            } else {
                menus.add(R.menu.change_source);
                if (hasAbility(capabilities, OrpheusApi.Ability.SETTINGS)) {
                    menus.add(R.menu.library_settings);
                }
            }

            int[] menusArray;
            if (!menus.isEmpty()) {
                menusArray = toArray(menus);
            } else {
                menusArray = new int[0];
            }
            ActionBarOwner.CustomMenuItem[] customMenuArray;
            if (!customMenus.isEmpty()) {
                customMenuArray = customMenus.toArray(new ActionBarOwner.CustomMenuItem[customMenus.size()]);
            } else {
                customMenuArray = new ActionBarOwner.CustomMenuItem[0];
            }

            return new ActionBarOwner.MenuConfig(createMenuActionHandler(), menusArray, customMenuArray);
        }

        Func1<Integer, Boolean> createMenuActionHandler() {
            return new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(final Integer integer) {
                    switch (integer) {
                        case R.id.menu_search:
                            bus.post(new MakeToast(R.string.err_unimplemented));
                            //TODO
                            return true;
                        case R.id.menu_change_source:
                            settings.removeDefaultLibraryInfo(pluginInfo);
                            if (getView() != null) {
                                AppFlow.get(getView().getContext()).resetTo(new PluginScreen(pluginInfo));
                            }
                            return true;
                        case R.id.menu_library_settings:
                            connection.getSettingsIntent(pluginInfo)
                                    .subscribe(new Action1<Intent>() {
                                        @Override
                                        public void call(Intent intent) {
                                            if (intent.getComponent() != null) {
                                                intent.putExtra(OrpheusApi.EXTRA_LIBRARY_ID, libraryInfo.libraryId);
                                                intent.putExtra(OrpheusApi.EXTRA_LIBRARY_INFO, libraryInfo);
                                                bus.post(new StartActivityForResult(intent,
                                                        StartActivityForResult.PLUGIN_REQUEST_SETTINGS));
                                            } else {
                                                bus.post(new MakeToast(R.string.err_opening_settings));
                                            }
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            bus.post(new MakeToast(R.string.err_opening_settings));
                                        }
                                    });
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

        boolean hasAbility(int capabilities, int ability) {
            return (capabilities & ability) != 0;
        }

        static int[] toArray(Collection<Integer> collection) {
            Object[] boxedArray = collection.toArray();
            int len = boxedArray.length;
            int[] array = new int[len];
            for (int i = 0; i < len; i++) {
                array[i] = ((Integer) boxedArray[i]).intValue();
            }
            return array;
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
                Timber.e(e, "ResultObserver.OnError()");
                if (retryCount++ >= RETRY_LIMIT) {
                    bus.post(new MakeToast(R.string.err_retrieving_items));
                    return;
                }
                Bundle token = lastResult != null ? lastResult.token : null;
                int backoff = (int) ((Math.pow(2, retryCount) + Math.random()) * 1000);
                Timber.d("retry=%d backoff=%d", retryCount, backoff);
                if (e instanceof RemoteException) {
                    connection.connectionManager.onException(pluginInfo.componentName);
                    loadMore(token, backoff);
                } else if (e instanceof LibraryConnection.ResultException) {
                    int code = ((LibraryConnection.ResultException) e).getCode();
                    switch (code) {
                        case OrpheusApi.Error.NETWORK:
                            //TODO
                            loadMore(token, backoff);
                            break;
                        case OrpheusApi.Error.AUTH_FAILURE:
                            settings.removeDefaultLibraryInfo(pluginInfo);
                            bus.post(new MakeToast(R.string.err_authentication));
                            if (getView() != null) {
                                AppFlow.get(getView().getContext()).resetTo(new PluginScreen(pluginInfo));
                            }
                            break;
                        case OrpheusApi.Error.RETRY:
                            loadMore(token, backoff);
                            break;
                        case OrpheusApi.Error.UNKNOWN:
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
