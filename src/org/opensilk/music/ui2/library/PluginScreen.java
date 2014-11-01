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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.ActivityBlueprint;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.util.PluginSettings;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library_plugin)
@WithModule(PluginScreen.Module.class)
public class PluginScreen extends Screen {

    public final PluginInfo plugin;

    public PluginScreen(PluginInfo plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return super.getName() + plugin.componentName;
    }

    @dagger.Module (
            addsTo = ActivityBlueprint.Module.class,
            injects = PluginView.class,
            library = true
    )
    public static class Module {
        final PluginScreen screen;

        public Module(PluginScreen screen) {
            this.screen = screen;
        }

        @Provides
        public PluginInfo providePluginInfo() {
            return screen.plugin;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<PluginView> {

        final PluginInfo plugin;
        final PluginSettings settings;
        final PluginConnectionManager connectionManager;
        final EventBus bus;
        final ActionBarOwner actionBarOwner;
        final Context appContext;

        String libraryIdentity;
        int capabilities;
        final Func1<Integer, Boolean> menuActionHandler;

        @Inject
        public Presenter(PluginInfo plugin, PluginSettings settings,
                         PluginConnectionManager connectionManager,
                         @Named("activity") EventBus bus,
                         ActionBarOwner actionBarOwner,
                         @ForApplication Context context) {
            this.plugin = plugin;
            this.settings = settings;
            this.connectionManager = connectionManager;
            this.bus = bus;
            this.actionBarOwner = actionBarOwner;
            this.appContext = context;

            menuActionHandler = createMenuActionHandler();
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
            bus.register(this);
            connectionManager.bind(plugin.componentName).subscribe(); //gets the ball rolling
        }

        @Override
        public void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            if (savedInstanceState != null) {
                libraryIdentity = savedInstanceState.getString("library_id");
            } else {
                libraryIdentity = settings.getDefaultSource();
            }
            setupActionBar();
            connect();
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
            outState.putString("library_id", libraryIdentity);
        }

        @Override
        protected void onExitScope() {
            Timber.v("onExitScope");
            super.onExitScope();
            bus.unregister(this);
        }

        void connect() {
            connectionManager.bind(plugin.componentName).subscribe(new Action1<RemoteLibrary>() {
                @Override
                public void call(RemoteLibrary remoteLibrary) {
                    onConnectionEstablished(remoteLibrary);
                }
            });
        }

        void onConnectionEstablished(RemoteLibrary remoteLibrary) {
            Timber.v("onConnectionEstablished()");
            if (getView() != null) {
                try {
                    int caps = remoteLibrary.getCapabilities();
//                    if (caps != capabilities) {
                        capabilities = caps;
                        setupActionBar();
//                    }
                    if (TextUtils.isEmpty(libraryIdentity)) {
                        Intent i = new Intent();
                        remoteLibrary.getLibraryChooserIntent(i);
                        if (i.getComponent() != null) {
                            bus.post(new StartActivityForResult(i, StartActivityForResult.PLUGIN_REQUEST_LIBRARY));
                        }
                    } else {
                        openLibrary();
                    }
                } catch (RemoteException e) {
                    //TODO
                    e.printStackTrace();
                }
            }
        }

        public void onEventMainThread(ActivityResult res) {
            Timber.v("onActivityResultEvent");
            switch (res.reqCode) {
                case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
                    if (res.resultCode == Activity.RESULT_OK) {
                        final String id = res.intent.getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
                        if (TextUtils.isEmpty(id)) {
                            Timber.e("Library chooser must set EXTRA_LIBRARY_ID");
                            //TODO toast
                            return;
                        }
                        libraryIdentity = id;
                        settings.setDefaultSource(libraryIdentity);
                        connect();
                    } else {
                        Timber.e("Activity returned bad result");
                        //TODO toast
                    }
                    break;
                case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                    break;
            }
        }

        void openLibrary() {
            Timber.v("openLibrary()");
            PluginView v = getView();
            if (v == null) return;
            LibraryInfo info = new LibraryInfo(libraryIdentity, plugin.componentName, null);
            LibraryScreen screen = new LibraryScreen(info);
            AppFlow.get(v.getContext()).goTo(screen);
        }

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config(true, true, plugin.title, createMenuConfig()));
        }

        ActionBarOwner.MenuConfig createMenuConfig(){
            List<Integer> menus = new ArrayList<>();
            List<ActionBarOwner.CustomMenuItem> customMenus = new ArrayList<>();

            // search
            if ((capabilities & OrpheusApi.Ability.SEARCH) != 0) {
                menus.add(R.menu.search);
            }

            Resources res = null;
            try {
                res = appContext.getPackageManager()
                        .getResourcesForApplication(plugin.componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {}

            if (res != null) {
                // device selection
                try {
                    String change = res.getString(res.getIdentifier("menu_change_source",
                            "string", plugin.componentName.getPackageName()));
                    customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_change_source, change));
                } catch (Resources.NotFoundException ignored) {
                    menus.add(R.menu.change_source);
                }
                // library settings
                if ((capabilities & OrpheusApi.Ability.SETTINGS) != 0) {
                    try {
                        String settings = res.getString(res.getIdentifier("menu_library_settings",
                                "string", plugin.componentName.getPackageName()));
                        customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_library_settings, settings));
                    } catch (Resources.NotFoundException ignored) {
                        menus.add(R.menu.library_settings);
                    }
                }
            } else {
                menus.add(R.menu.change_source);
                if ((capabilities & OrpheusApi.Ability.SETTINGS) != 0) {
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

            return new ActionBarOwner.MenuConfig(menuActionHandler, menusArray, customMenuArray);
        }

        Func1<Integer, Boolean> createMenuActionHandler() {
            return new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    switch (integer) {
                        case R.id.menu_search:
                            return true;
                        case R.id.menu_change_source:
                            settings.clearDefaultSource();
                            //TODO reset flow
                            return true;
                        case R.id.menu_library_settings:
                            connectionManager.bind(plugin.componentName).subscribe(new Action1<RemoteLibrary>() {
                                @Override
                                public void call(RemoteLibrary remoteLibrary) {
                                    try {
                                        Intent i = new Intent();
                                        remoteLibrary.getSettingsIntent(i);
                                        if (i.getComponent() != null) {
                                            i.putExtra(OrpheusApi.EXTRA_LIBRARY_ID, libraryIdentity);
                                            bus.post(new StartActivityForResult(i, StartActivityForResult.PLUGIN_REQUEST_SETTINGS));
                                        }
                                    } catch (RemoteException | NullPointerException e) {
                                        //TODO toast
                                    }
                                }
                            });
                            return true;
                        default:
                            return false;
                    }
                }
            };
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

    }

}
