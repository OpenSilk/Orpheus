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

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
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
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library_plugin)
@WithModule(PluginScreen.Module.class)
@WithTransitions(
        single = R.anim.grow_fade_in,
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.shrink_fade_out, R.anim.slide_in_left }
)
public class PluginScreen extends Screen {

    @dagger.Module (
            addsTo = LibrarySwitcherScreen.Module.class,
            injects = PluginView.class,
            library = true
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<PluginView> {

        final PluginInfo plugin;
        final PluginSettings settings;
        final LibraryConnection connection;
        final EventBus bus;
        final ActionBarOwner actionBarOwner;
        final Context appContext;

        String libraryIdentity;

        @Inject
        public Presenter(PluginInfo plugin, PluginSettings settings,
                         LibraryConnection connection,
                         @Named("activity") EventBus bus,
                         ActionBarOwner actionBarOwner,
                         @ForApplication Context context) {
            this.plugin = plugin;
            this.settings = settings;
            this.connection = connection;
            this.bus = bus;
            this.actionBarOwner = actionBarOwner;
            this.appContext = context;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
            bus.register(this);
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
            if (!TextUtils.isEmpty(libraryIdentity)) {
                openLibrary();
            } else {
                connection.getLibraryChooserIntent().subscribe(new Action1<Intent>() {
                    @Override
                    public void call(Intent intent) {
                        if (intent.getComponent() != null) {
                            bus.post(new StartActivityForResult(intent, StartActivityForResult.PLUGIN_REQUEST_LIBRARY));
                        }
                    }
                });
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
            LibraryInfo info = new LibraryInfo(libraryIdentity, null, null, null);
            LibraryScreen screen = new LibraryScreen(info);
            AppFlow.get(v.getContext()).goTo(screen);
        }

        void setupActionBar() {
//            actionBarOwner.setConfig(new ActionBarOwner.Config(true, true, plugin.title, createMenuConfig()));
        }

    }

}
