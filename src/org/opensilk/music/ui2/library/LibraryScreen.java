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

import com.andrew.apollo.R;

import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.library_list)
public class LibraryScreen implements Blueprint {

    final LibraryInfo info;

    public LibraryScreen(LibraryInfo info) {
        this.info = info;
    }

    @Override
    public String getMortarScopeName() {
        return getClass().getName() + info.toString();
    }

    @Override
    public Object getDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            injects = LibraryView.class,
            addsTo = PluginScreen.Module.class,
            library = true
    )
    public static class Module {

        final LibraryScreen screen;

        public Module(LibraryScreen screen) {
            this.screen = screen;
        }

        @Provides
        public LibraryInfo provideLibraryInfo() {
            return screen.info;
        }

    }

    @Singleton
    public static class Presenter extends ViewPresenter<LibraryView> {

        final Flow flow;
        final LibraryLoader loader;
        final LibraryInfo info;

        final ResultObserver observer;

        @Inject
        public Presenter(@Named("plugin") Flow flow, LibraryLoader loader, LibraryInfo info) {
            this.flow = flow;
            this.loader = loader;
            this.info = info;

            observer = new ResultObserver();
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
        }


        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
            subscribe(null);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
        }

        @DebugLog
        public boolean fetchMore(Bundle token) {
            if (!observer.complete) return false;
            subscribe(token);
            return true;
        }

        private void subscribe(Bundle token) {
            observer.complete = false;
            loader.getObservable(token).subscribe(observer);
        }

        public void go(Bundleable item) {
            Timber.v("go(%s)", item);
            //FRP thingy to avoid final
            Observable<Bundleable> og = Observable.just(item);
            // we need to convert the generic Bundleable into an action we can use
            // to proceed to the next screen, we first create separate observables
            // for each type of object, that filters for that type, then casts to
            // the appropriate type, and finally maps the type into a generic action0
            // that moves us into the next screen
            Observable<? extends Action0> folder = og.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    Timber.v("filter folder");
                    return (bundleable instanceof Folder);
                }
            }).cast(Folder.class).flatMap(new Func1<Folder, Observable<? extends Action0>>() {
                @Override
                public Observable<? extends Action0> call(final Folder folder) {
                    return Observable.just(new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, folder.identity)));
                        }
                    });
                }
            });
            Observable<? extends Action0> song = og.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    Timber.v("filter song");
                    return (bundleable instanceof Song);
                }
            }).cast(Song.class).flatMap(new Func1<Song, Observable<? extends Action0>>() {
                @Override
                public Observable<? extends Action0> call(final Song song) {
                    return Observable.just(new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, song.identity)));
                        }
                    });
                }
            });
            Observable<? extends Action0> artist = og.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    Timber.v("filter artist");
                    return (bundleable instanceof Artist);
                }
            }).cast(Artist.class).flatMap(new Func1<Artist, Observable<? extends Action0>>() {
                @Override
                public Observable<? extends Action0> call(final Artist artist) {
                    return Observable.just(new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, artist.identity)));
                        }
                    });
                }
            });
            Observable<? extends Action0> album = og.filter(new Func1<Bundleable, Boolean>() {
                @Override
                public Boolean call(Bundleable bundleable) {
                    Timber.v("filter album");
                    return (bundleable instanceof Album);
                }
            }).cast(Album.class).flatMap(new Func1<Album, Observable<? extends Action0>>() {
                @Override
                public Observable<? extends Action0> call(final Album album) {
                    return Observable.just(new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, album.identity)));
                        }
                    });
                }
            });
            // finally merge the previous Observables into a single operation
            Observable.merge(folder, song, artist, album)
                    .first()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Action0>() {
                        @Override
                        public void call(Action0 action0) {
                            Timber.v("call action0");
                            action0.call();
                        }
                    });
        }

        private class ResultObserver implements Observer<LibraryLoader.Result> {

            boolean complete;

            @Override
            public void onCompleted() {
                complete = true;
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(LibraryLoader.Result result) {
                Timber.v("onNext called from:" + Thread.currentThread().getName());
                LibraryView v = getView();
                if (v == null) return;
                v.getAdapter().onNewResult(result);
            }
        }

    }

}
