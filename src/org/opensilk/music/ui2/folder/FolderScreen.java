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
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.MediaProviderUtil;
import org.opensilk.music.R;
import org.opensilk.music.ui.folder.FolderPickerActivity;
import org.opensilk.music.ui2.BaseSwitcherActivityBlueprint;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.folder_list)
@WithModule(FolderScreen.Module.class)
@WithTransitions(
        single = R.anim.grow_fade_in,
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.shrink_fade_out, R.anim.slide_in_left }
)
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
            addsTo = BaseSwitcherActivityBlueprint.Module.class,
            injects = FolderView.class
    )
    public static class Module {
        final FolderScreen screen;

        public Module(FolderScreen screen) {
            this.screen = screen;
        }

        @Provides @Named("directory")
        public String provideDirectory() {
            return screen.directory;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<FolderView> {

        final Loader loader;
        Subscription subscription;

        @Inject
        public Presenter(Loader loader) {
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
            if (subscription != null) subscription.unsubscribe();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            getView().setup();
            subscription = loader.getObservable().subscribe(new Action1<List<FileItem>>() {
                @Override
                public void call(List<FileItem> fileItems) {
                    FolderView v = getView();
                    if (v == null) return;
                    v.getAdapter().addAll(fileItems);
                }
            });
        }

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
        }

        public void go(Context context, FileItem item) {
            Timber.v("go(%s)", item);
            FolderView v = getView();
            if (v == null) return;
            AppFlow.get(context).goTo(new FolderScreen(item.getPath()));
        }

    }

    @Singleton
    static class Loader {
        final Context context;
        final String directory;

        final static Set<Integer> MEDIA_TYPES = new HashSet<>();
        static {
            MEDIA_TYPES.add(FileItem.MediaType.AUDIO);
            MEDIA_TYPES.add(FileItem.MediaType.DIRECTORY);
        }

        @Inject
        public Loader(@ForApplication Context context, @Named("directory") String directory) {
            this.context = context;
            this.directory = directory.endsWith("/") ? directory.substring(0, directory.length()-1) : directory;
        }

        public Observable<List<FileItem>> getObservable() {
            return Observable.create(new Observable.OnSubscribe<List<FileItem>>() {
                @Override
                public void call(Subscriber<? super List<FileItem>> subscriber) {
                    try {
                        List<FileItem> items = MediaProviderUtil.ls(context, directory, MEDIA_TYPES);
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(items);
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(e);
                    }
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }

    }
}
