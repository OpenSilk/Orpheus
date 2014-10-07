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
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.andrew.apollo.R;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.main.DrawerView;
import org.opensilk.music.ui2.main.God;
import org.opensilk.music.util.PluginSettings;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library)
public class PluginScreen implements Blueprint {

    final PluginInfo plugin;

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
            addsTo = God.Module.class,
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

        @Provides
        public RemoteLibrary provideLibraryConnection(Presenter presenter) {
            return presenter.getLibraryConnection();
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<PluginView> implements PluginConnection.Listener {

        final PluginConnection connection;
        final PluginInfo plugin;
        final PluginSettings settings;
        final Bus bus;

        String libraryIdentity;
        boolean loaded;

        @Inject
        public Presenter(@ForApplication Context context, PluginInfo plugin,
                         PluginConnection connection, @Named("activity") Bus bus) {
            this.connection = connection;
            this.plugin = plugin;
            this.settings = new PluginSettings(context, plugin.componentName);
            this.bus = bus;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            bus.register(this);
            connection.connect(this);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            if (savedInstanceState != null) {
                libraryIdentity = savedInstanceState.getString("library_id");
            }
            loaded = true;
            if (connection.isConnected()) {
                onConnectionEstablished();
            }
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
            outState.putString("library_id", libraryIdentity);
            loaded = false;
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            bus.unregister(this);
            connection.disconnect();
        }

        @Override
        public void onConnectionEstablished() {
            if (loaded) {
                try {
                    if (TextUtils.isEmpty(libraryIdentity)) {
                        Intent i = new Intent();
                        getLibraryConnection().getLibraryChooserIntent(i);
                        if (i.getComponent() != null) {
                            bus.post(new StartActivityForResult(i, StartActivityForResult.PLUGIN_REQUEST_LIBRARY));
                        }
                    } else {
                        openLibrary();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onConnectionLost() {

        }

        @Subscribe
        public void onActivityResultEvent(ActivityResult res) {
            switch (res.reqCode) {
                case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
                    if (res.resultCode == Activity.RESULT_OK) {
                        final String id = res.intent.getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
                        if (TextUtils.isEmpty(id)) {
                            Timber.e("Library chooser must set EXTRA_LIBRARY_ID");
                            return;
                        }
                        libraryIdentity = id;
                        settings.setDefaultSource(libraryIdentity);
                        onConnectionEstablished();
                    } else {
                        //TODO
                    }
                    break;
                case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                    break;
            }
        }

        private void openLibrary() {
            LibraryInfo info = new LibraryInfo(libraryIdentity, plugin.componentName, null);
            DrawerView.ScreenConductor.addChild(getView().getContext(), new LibraryScreen(info), getView());
        }

        public RemoteLibrary getLibraryConnection() {
            return connection.getConnection();
        }
    }

}
