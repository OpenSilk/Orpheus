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

import android.os.Bundle;

import com.andrew.apollo.R;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.music.loader.AsyncLoader;
import org.opensilk.music.loader.FileItemLoader;
import org.opensilk.music.ui.folder.FolderPickerActivity;
import org.opensilk.music.ui2.main.GodScreen;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import mortar.Blueprint;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.folder_list)
public class FolderScreen implements Blueprint {

    final String directory;

    public FolderScreen() {
        this(FolderPickerActivity.SDCARD_ROOT);
    }

    public FolderScreen(String directory) {
        this.directory = directory;
    }

    @Override
    public String getMortarScopeName() {
        return getClass().getName() + directory;
    }

    @Override
    public Object getDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            injects = FolderView.class,
            addsTo = GodScreen.Module.class
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

        final Flow flow;
        final FileItemLoader loader;

        @Inject
        public Presenter(Flow flow, FileItemLoader loader) {
            this.flow = flow;
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
            loader.loadAsync(this);
        }

        @Override
        protected void onSave(Bundle outState) {
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

        public void go(FileItem item) {
            Timber.v("go(%s)", item);
            flow.goTo(new FolderScreen(item.getPath()));
        }

    }
}
