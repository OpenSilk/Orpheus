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
import org.opensilk.music.ui2.core.FlowOwner;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.main.DrawerView;
import org.opensilk.music.ui2.main.God;
import org.opensilk.music.util.PluginSettings;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Backstack;
import flow.Flow;
import flow.Layout;
import flow.Parcer;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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

        @Provides @Named("plugin")
        public Flow provideFlow(Presenter presenter) {
            return presenter.getFlow();
        }

    }

    @Singleton
    public static class Presenter extends FlowOwner<Blueprint, PluginView> implements PluginConnection.Listener {

        final PluginConnection connection;
        final PluginInfo plugin;
        final PluginSettings settings;
        final Bus bus;

        String libraryIdentity;

        @Inject
        public Presenter(Parcer<Object> parcer, PluginConnection connection, PluginInfo plugin,
                         PluginSettings settings, @Named("activity") Bus bus) {
            super(parcer);
            this.connection = connection;
            this.plugin = plugin;
            this.settings = settings;
            this.bus = bus;
        }

        @Override
        protected Blueprint getFirstScreen() {
            return null; //Defer flo creation
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
            bus.register(this);
            connection.connect(this);
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
            if (connection.isConnected()) {
                onConnectionEstablished();
            }
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
            connection.disconnect();
        }

        @Override
        public void onConnectionEstablished() {
            Timber.v("onConnectionEstablished()");
            if (getView() != null) {
                try {
                    if (TextUtils.isEmpty(libraryIdentity)) {
                        Intent i = new Intent();
                        if (!connection.isConnected()) throw new RemoteException();
                        connection.getConnection().getLibraryChooserIntent(i);
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

        @Override
        public void onConnectionLost() {
            Observable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            connection.connect(Presenter.this);
                        }
                    });
//            flow.resetTo(new PluginScreen(plugin));
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
                        onConnectionEstablished();
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
            if (flow == null) {
                LibraryInfo info = new LibraryInfo(libraryIdentity, plugin.componentName, null);
                LibraryScreen screen = new LibraryScreen(info);
                Backstack backstack = Backstack.single(screen);
                flow = new Flow(backstack, this);
                showScreen((Blueprint) flow.getBackstack().current().getScreen(), null);
            }
//            DrawerView.ScreenConductor.addChild(getView().getContext(), new LibraryScreen(info), getView());
        }

    }

}
