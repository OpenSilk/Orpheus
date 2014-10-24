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

package org.opensilk.music.ui2.folder;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.music.loader.AsyncLoader;
import org.opensilk.music.loader.FileItemLoader;
import org.opensilk.music.ui.folder.FolderPickerActivity;
import org.opensilk.music.ui2.ActivityBlueprint;
import org.opensilk.music.ui2.main.MainViewBlueprint;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.folder_list)
@WithModule(FolderScreen.Module.class)
public class FolderScreen extends Screen {

    final String directory;

    public FolderScreen() {
        this(FolderPickerActivity.SDCARD_ROOT);
    }

    public FolderScreen(String directory) {
        this.directory = directory;
    }

    @Override
    public String getName() {
        return super.getName() + directory;
    }

    @dagger.Module(
            addsTo = ActivityBlueprint.Module.class,
            injects = FolderView.class
    )
    public static class Module {
        final FolderScreen screen;

        public Module(FolderScreen screen) {
            this.screen = screen;
        }

        @Provides
        public String provideDirectory() {
            return screen.directory;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<FolderView> implements AsyncLoader.Callback<FileItem> {

        final FileItemLoader loader;

        @Inject
        public Presenter(FileItemLoader loader) {
            Timber.v("new Presenter(Folder)");
            this.loader = loader;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope()");
            super.onEnterScope(scope);
        }

        @Override
        protected void onExitScope() {
            Timber.v("onExitScope()");
            super.onExitScope();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            getView().setup();
            loader.loadAsync(this);
        }

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
        }

        @Override
        public void onDataFetched(List<FileItem> items) {
            FolderView v = getView();
            if (v != null) {
                v.getAdapter().clear();
                v.getAdapter().addAll(items);
            }
        }

        public void go(Context context, FileItem item) {
            Timber.v("go(%s)", item);
            FolderView v = getView();
            if (v == null) return;
            AppFlow.get(context).goTo(new FolderScreen(item.getPath()));
        }

    }
}
