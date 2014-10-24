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

import org.opensilk.music.R;

import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.ActivityBlueprint;
import org.opensilk.music.ui2.main.MainViewBlueprint;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
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
            addsTo = ActivityBlueprint.Module.class,
            injects = LibraryView.class,
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
        public Presenter(Flow flow, LibraryLoader loader, LibraryInfo info) {
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

        // I know this seems ridiculous an if else block would be more sane
        // but im still trying to learn how FRP works and wanted to do this with it.
        public void go(Bundleable item) {
            Timber.v("go(%s)", item);
            //FRP thingy to avoid final
            Observable<Bundleable> og = Observable.just(item);
            // we need to convert the generic Bundleable into an action we can use
            // to proceed to the next screen, we first create separate observables
            // for each type of object, that filters for that type, then casts to
            // the appropriate type, and finally maps the type into a generic action0
            // that moves us into the next screen
            Observable<? extends Action0> folder = og.ofType(Folder.class).cast(Folder.class).map(new Func1<Folder, Action0>() {
                @Override
                public Action0 call(final Folder folder) {
                    return new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, folder.identity)));
                        }
                    };
                }
            });
            Observable<? extends Action0> song = og.ofType(Song.class).cast(Song.class).map(new Func1<Song, Action0>() {
                @Override
                public Action0 call(final Song song) {
                    return new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, song.identity)));
                        }
                    };
                }
            });
            Observable<? extends Action0> artist = og.ofType(Artist.class).cast(Artist.class).map(new Func1<Artist, Action0>() {
                @Override
                public Action0 call(final Artist artist) {
                    return new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, artist.identity)));
                        }
                    };
                }
            });
            Observable<? extends Action0> album = og.ofType(Album.class).cast(Album.class).map(new Func1<Album, Action0>() {
                @Override
                public Action0 call(final Album album) {
                    return new Action0() {
                        @Override
                        public void call() {
                            flow.goTo(new LibraryScreen(new LibraryInfo(info.libraryId, info.libraryComponent, album.identity)));
                        }
                    };
                }
            });
            // finally merge the previous Observables into a single operation
            Observable.merge(folder, song, artist, album)
                    // since we only started with one item we can safely just grab
                    // the first Action0 that is emmited
                    .first()
                    // I think it will already be observed on main
                    // In fact the whole thing /should/ be synchronous
                    // but just in case
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Action0>() {
                        @Override
                        public void call(Action0 action0) {
//                            Timber.v("call action0");
                            action0.call();
                        }
                    });
        }

        // we re use this so we cant use a subscriber
        // not that it matters since you cant cancel an Observable created from
        // a future (noted for future reference (damn puns))
        private class ResultObserver implements Observer<LibraryLoader.Result> {

            boolean complete;

            @Override
            public void onCompleted() {
                complete = true;
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "LibraryLoader.Result Observer");
                //TODO
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
