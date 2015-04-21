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

package org.opensilk.music.ui2.search;

import android.os.Bundle;
import android.text.TextUtils;

import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.library.LibraryConnection;
import org.opensilk.music.ui2.loader.PluginLoader;
import org.opensilk.music.ui2.loader.SearchLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class SearchScreenPresenter extends ViewPresenter<SearchListView> {

    final ActionBarOwner actionBarOwner;
    final LibraryConnection libraryConnection;
    final PluginLoader pluginLoader;
    final AppPreferences settings;
    final SearchLoader searchLoader;

    Set<PluginHolder> searchablePlugins;
    CompositeSubscription subscriptions;
    volatile boolean pluginsQueried;

    @Inject
    public SearchScreenPresenter(ActionBarOwner actionBarOwner,
                                 LibraryConnection libraryConnection,
                                 PluginLoader pluginLoader,
                                 AppPreferences settings,
                                 SearchLoader searchLoader) {
        this.actionBarOwner = actionBarOwner;
        this.libraryConnection = libraryConnection;
        this.pluginLoader = pluginLoader;
        this.settings = settings;
        this.searchLoader = searchLoader;

        searchablePlugins = Collections.synchronizedSet(new LinkedHashSet<PluginHolder>());
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        final Scheduler.Worker worker = Schedulers.io().createWorker();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                getSearchablePlugins();
                worker.unsubscribe();
            }
        });
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (savedInstanceState != null) {
            getView().filterString = savedInstanceState.getString("query");
        }
        setupActionBar();
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        outState.putString("query", getView() != null ? getView().filterString : null);
    }

    void setupActionBar() {
        actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                        .setUpButtonEnabled(true)
                        .setMenuConfig(new ActionBarOwner.MenuConfig.Builder()
                                        .withMenus(R.menu.searchview)
                                        .setActionHandler(new Func1<Integer, Boolean>() {
                                            @Override
                                            public Boolean call(Integer integer) {
                                                return false;
                                            }
                                        })
                                        .build()
                        )
                        .build()
        );
    }

    @DebugLog
    void restartLoaders(final String query) {
        if (!pluginsQueried) {
            return;
        }
        if (isSubscribed(subscriptions)) {
            subscriptions.unsubscribe();
            subscriptions = null;
        }
        subscriptions = new CompositeSubscription();

        subscriptions.add(
                searchLoader.setFilter(query).getListObservable().subscribe(
                        new SimpleObserver<List<Object>>() {
                            @Override
                            public void onNext(List<Object> objects) {
                                if (getView() != null) {
                                    getView().adapter.addItem(new SearchAdapter.ListHeader(
                                            getView().getContext().getString(R.string.my_library)
                                    ));
                                    getView().adapter.addAll(objects);
                                }
                            }

                            @Override
                            public void onCompleted() {
                                if (getView() != null) {
                                    getView().setListShown(true, true);
                                }
                            }
                        }
                )
        );

        for (final PluginHolder holder : searchablePlugins) {
            Subscription s = libraryConnection.search(holder.pluginInfo, holder.libraryInfo, query)
                    .map(new Func1<LibraryConnection.Result, List<BundleableHolder>>() {
                        @Override
                        public List<BundleableHolder> call(LibraryConnection.Result result) {
                            List<BundleableHolder> list = new ArrayList<>(result.items.size());
                            for (Bundleable b : result.items) {
                                list.add(new BundleableHolder(holder, b));
                            }
                            return list;
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<List<BundleableHolder>>() {
                        @Override
                        public void onNext(List<BundleableHolder> list) {
                            if (getView() != null) {
                                getView().adapter.addItem(new SearchAdapter.ListHeader(
                                        holder.pluginInfo.title.toString()
                                ));
                                getView().adapter.addAll(list);
                            }
                        }

                        @Override
                        public void onCompleted() {
                            if (getView() != null) {
                                getView().setListShown(true, true);
                            }
                        }
                    });
            subscriptions.add(s);
        }
    }

    Subscription searchableSubscription;

    @DebugLog
    void getSearchablePlugins() {
        searchableSubscription = pluginLoader.getObservable().subscribeOn(Schedulers.io()).subscribe(
                new SimpleObserver<List<PluginInfo>>() {
                    @Override
                    public void onNext(List<PluginInfo> pluginInfos) {
                        for (PluginInfo p : pluginInfos) {
                            LibraryInfo i = settings.getDefaultLibraryInfo(p);
                            if (i != null) {
                                PluginConfig c = libraryConnection.getConfig(p).toBlocking().first();
                                if (c.hasAbility(PluginConfig.SEARCHABLE)) {
                                    searchablePlugins.add(new PluginHolder(p, c, i));
                                }
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                        pluginsQueried = true;
                        if (getView() != null && !TextUtils.isEmpty(getView().filterString)) {
                            final String query = getView().filterString;
                            getView().post(new Runnable() {
                                @Override
                                public void run() {
                                    restartLoaders(query);
                                }
                            });
                        }
                    }
                }
        );
    }
}
