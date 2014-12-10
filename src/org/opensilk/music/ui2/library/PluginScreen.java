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
import android.os.Parcel;
import android.text.TextUtils;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.ActivityResult;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.StartActivityForResult;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library_plugin)
@WithModule(PluginScreen.Module.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.shrink_fade_out, R.anim.slide_in_left }
)
public class PluginScreen extends Screen {

    final PluginInfo pluginInfo;

    public PluginScreen(PluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }

    @Override
    public String getName() {
        return super.getName() + pluginInfo.componentName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(pluginInfo, flags);
        super.writeToParcel(dest, flags);
    }

    @dagger.Module (
            addsTo = LauncherActivity.Module.class,
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
            return screen.pluginInfo;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<PluginView> {

        final PluginInfo pluginInfo;
        final AppPreferences settings;
        final LibraryConnection connection;
        final EventBus bus;
        final ActionBarOwner actionBarOwner;
        final Context appContext;

        LibraryInfo libraryInfo;
        PluginConfig pluginConfig;

        @Inject
        public Presenter(PluginInfo pluginInfo,
                         AppPreferences settings,
                         LibraryConnection connection,
                         @Named("activity") EventBus bus,
                         ActionBarOwner actionBarOwner,
                         @ForApplication Context appContext) {
            this.pluginInfo = pluginInfo;
            this.settings = settings;
            this.connection = connection;
            this.bus = bus;
            this.actionBarOwner = actionBarOwner;
            this.appContext = appContext;
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
            setupActionBar();
            if (savedInstanceState != null) {
                libraryInfo = savedInstanceState.getParcelable("libraryinfo");
                Bundle b = savedInstanceState.getBundle("pluginConfig");
                if (b != null) pluginConfig = PluginConfig.materialize(b);
                if (pluginConfig == null) {
                    getConfig();
                } else {
                    checkForDefaultLibrary();
                }
            } else {
                libraryInfo = settings.getDefaultLibraryInfo(pluginInfo);
                getConfig();
            }
        }

        @Override
        public void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
            outState.putParcelable("libraryinfo", libraryInfo);
            outState.putBundle("pluginConfig", pluginConfig != null ? pluginConfig.dematerialize() : null);
        }

        @Override
        protected void onExitScope() {
            Timber.v("onExitScope");
            super.onExitScope();
            bus.unregister(this);
        }

        void getConfig() {
            if (isApi010()) return;
            connection.getConfig(pluginInfo).subscribe(new Action1<PluginConfig>() {
                @Override
                public void call(PluginConfig config) {
                    Presenter.this.pluginConfig = config;
                    checkForDefaultLibrary();
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    bus.post(new MakeToast(R.string.err_connecting_library));
                }
            });
        }

        boolean isApi010() {
            // Api 010 doesnt have a getConfig() method it also doesnt
            // define any permissions so im using the lack of permissions
            // as an indication of api 010 and alerting the user to upgrade;
            if (pluginInfo.hasPermission) return false;
            if (getView() != null) getView().showUpgradeAlert(
                    pluginInfo.title.toString(),
                    pluginInfo.componentName.getPackageName()
            );
            return true;
        }

        void checkForDefaultLibrary() {
            if (libraryInfo != null) {
                openLibrary();
            } else {
                openPicker();
            }
        }

        void openPicker() {
            bus.post(new StartActivityForResult(new Intent().setComponent(pluginConfig.pickerComponent),
                    StartActivityForResult.PLUGIN_REQUEST_LIBRARY));
        }

        void openLibrary() {
            Timber.v("openLibrary()");
            PluginView v = getView();
            if (v == null) return;
            LibraryScreen screen = new LibraryScreen(pluginInfo, pluginConfig, libraryInfo);
            AppFlow.get(v.getContext()).replaceTo(screen);
        }

        void setupActionBar() {
            actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
                    .setTitle(pluginInfo.title)
                    .build());
        }

        void showPickerButton() {
            if (getView() == null) return;
            getView().showLanding();
        }

        public void onEventMainThread(ActivityResult res) {
            Timber.v("onActivityResultEvent");
            switch (res.reqCode) {
                case StartActivityForResult.PLUGIN_REQUEST_LIBRARY:
                    if (res.resultCode == Activity.RESULT_OK) {
                        libraryInfo = res.intent.getParcelableExtra(OrpheusApi.EXTRA_LIBRARY_INFO);
                        if (libraryInfo == null) {
                            Timber.e("Library chooser should set EXTRA_LIBRARY_INFO");
                            String id = res.intent.getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
                            if (TextUtils.isEmpty(id)) {
                                Timber.e("Library chooser must set EXTRA_LIBRARY_ID");
                                bus.post(new MakeToast(R.string.err_connecting_library));
                                showPickerButton();
                                return;
                            }
                            libraryInfo = new LibraryInfo(id, null, null, null);
                        }
                        if (libraryInfo.folderId != null || libraryInfo.folderName != null) {
                            Timber.w("Please stop setting folderId and folderName in the returned LibraryInfo");
                            libraryInfo = libraryInfo.buildUpon(null, null);
                        }
                        settings.setDefaultLibraryInfo(pluginInfo, libraryInfo);
                        openLibrary();
                    } else {
                        Timber.e("Activity returned bad result");
                        bus.post(new MakeToast(R.string.err_connecting_library));
                        showPickerButton();
                    }
                    return;
                case StartActivityForResult.PLUGIN_REQUEST_SETTINGS:
                    return;
            }
        }

    }

    public static final Creator<PluginScreen> CREATOR = new Creator<PluginScreen>() {
        @Override
        public PluginScreen createFromParcel(Parcel source) {
            PluginScreen s = new PluginScreen(
                    source.<PluginInfo>readParcelable(null)
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public PluginScreen[] newArray(int size) {
            return new PluginScreen[size];
        }
    };

}
