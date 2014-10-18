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
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.opensilk.music.R;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.main.MainScreen;
import org.opensilk.music.util.PluginSettings;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library)
public class PluginScreen implements Blueprint {

    public final PluginInfo plugin;

    public PluginScreen(PluginInfo plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getMortarScopeName() {
        return getClass().getName() + plugin.componentName;
    }

    @Override
    public Object getDaggerModule() {
        return new Module(this);
    }

    @dagger.Module (
            addsTo = MainScreen.Module.class,
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
        final Bus bus;
        final Flow flow;

        String libraryIdentity;

        @Inject
        public Presenter(PluginInfo plugin, PluginSettings settings,
                         PluginConnectionManager connectionManager,
                         @Named("activity") Bus bus, Flow flow) {
            this.plugin = plugin;
            this.settings = settings;
            this.connectionManager = connectionManager;
            this.bus = bus;
            this.flow = flow;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
            bus.register(this);
            connectionManager.bind(plugin.componentName); //gets the ball rolling
        }

        @Override
        public void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            if (savedInstanceState != null) {
                libraryIdentity = savedInstanceState.getString("library_id");
            } else {
//                libraryIdentity = settings.getDefaultSource();
            }
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

        @Subscribe
        public void onActivityResultEvent(ActivityResult res) {
            Timber.v("onActivityResultEvent");
            switch (res.reqCode) {
                case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
                    if (res.resultCode == Activity.RESULT_OK) {
                        final String id = res.intent.getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
                        if (TextUtils.isEmpty(id)) {
                            Timber.e("Library chooser must set EXTRA_LIBRARY_ID");
                            //TODO notify user
                            return;
                        }
                        libraryIdentity = id;
                        settings.setDefaultSource(libraryIdentity);
                        connect();
                    } else {
                        Timber.e("Activity returned bad result");
                        //TODO
                    }
                    break;
                case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                    break;
            }
        }

        private void openLibrary() {
            Timber.v("openLibrary()");
            LibraryInfo info = new LibraryInfo(libraryIdentity, plugin.componentName, null);
            LibraryScreen screen = new LibraryScreen(info);
            flow.goTo(screen);
        }

    }

}
