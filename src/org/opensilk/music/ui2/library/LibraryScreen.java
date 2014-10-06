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

import android.os.Bundle;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.loader.EndlessAsyncLoader;
import org.opensilk.music.loader.EndlessRemoteAsyncLoader;
import org.opensilk.music.loader.FileItemLoader;
import org.opensilk.music.loader.LibraryLoader;
import org.opensilk.music.ui2.folder.FolderView;
import org.opensilk.music.ui2.main.God;
import org.opensilk.music.ui2.main.GodScreen;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
public class LibraryScreen implements Blueprint {

    final PluginInfo plugin;

    public LibraryScreen(PluginInfo plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getMortarScopeName() {
        return getClass().getName() + plugin.componentName;
    }

    @Override
    public Object getDaggerModule() {
        return null;
    }

    @dagger.Module(
            injects = LibraryView.class,
            addsTo = God.Module.class,
            library = true
    )
    public static class Module {

        final LibraryScreen screen;

        public Module(LibraryScreen screen) {
            this.screen = screen;
        }

        @Provides
        public PluginInfo provideLibrary() {
            return screen.plugin;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<LibraryView> implements EndlessRemoteAsyncLoader.Callback<Bundleable> {

        final Flow flow;
        final LibraryLoader loader;

        @Inject
        public Presenter(Flow flow, LibraryLoader loader) {
            this.flow = flow;
            this.loader = loader;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            loader.connect();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            loader.loadAsync(this);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            loader.disconnect();
        }

        @Override
        public void onDataFetched(List<Bundleable> items) {

        }

        @Override
        public void onMoreDataFetched(List<Bundleable> items) {

        }

        @Override
        public void onConnectionAvailable() {

        }

        public void go() {
        }

    }

}
