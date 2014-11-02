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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.R;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.ActivityBlueprint;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.Subject;
import timber.log.Timber;

import static org.opensilk.music.util.RxUtil.isSubscribed;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.library)
@WithModule(LibraryScreen.Module.class)
public class LibraryScreen extends Screen {

    final LibraryInfo info;

    public LibraryScreen(LibraryInfo info) {
        this.info = info;
    }

    @Override
    public String getName() {
        return super.getName() + info.toString();
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

        final LibraryConnection loader;
        final LibraryInfo info;
        final ArtworkRequestManager requestor;

        final ResultObserver resultObserver;
        Subscription resultSubscription;
        boolean isloading;
        boolean initialLoad = true;

        @Inject
        public Presenter(LibraryConnection loader,
                         LibraryInfo info,
                         ArtworkRequestManager requestor) {
            this.loader = loader;
            this.info = info;
            this.requestor = requestor;

            resultObserver = new ResultObserver();
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope");
            super.onEnterScope(scope);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            loadMore(null);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            if (isSubscribed(resultSubscription)) {
                resultSubscription.unsubscribe();
                resultSubscription = null;
            }
        }

        public void loadMore(Bundle token) {
            if (isloading) return;
            loadMore(token, 0);
        }

        public void loadMore(final Bundle token, long delayMilli) {
            Timber.v("loadMore()");
            LibraryView v = getView();
            if (v != null) {
                if (initialLoad) {
                    v.setLoading(true);
                } else {
                    v.setMoreLoading(true);
                }
            }
            if (isSubscribed(resultSubscription)) resultSubscription.unsubscribe();
            resultSubscription = Observable.timer(delayMilli, TimeUnit.MILLISECONDS)
                    .flatMap(new Func1<Long, Observable<LibraryConnection.Result>>() {
                        @Override
                        public Observable<LibraryConnection.Result> call(Long aLong) {
                            return loader.browse(token);
                        }
                    })
                    .subscribe(resultObserver);
        }

        public void onNewResult(LibraryConnection.Result result) {
            isloading = false;
            LibraryView v = getView();
            if (v == null) return;
            if (initialLoad) {
                initialLoad = false;
                if (result.items.isEmpty()) {
                    v.setListEmpty(true, true);
                } else {
                    v.setListShown(true, true);
                }
            } else {
                v.setMoreLoading(false);
            }
            v.adapter.onNewResult(result);
        }

        public void onItemClicked(Context context, Bundleable item) {
            String identity = null;
            if (item instanceof Folder) {
                identity = ((Folder) item).identity;
            } else if (item instanceof Album) {
                identity = ((Album) item).identity;
            } else if (item instanceof Artist) {
                 identity = ((Artist) item).identity;
            } else if (item instanceof Song) {
                return;
            }
            if (TextUtils.isEmpty(identity)) return;
            LibraryInfo newInfo = new LibraryInfo(info.libraryId, info.libraryComponent, identity);
            AppFlow.get(context).goTo(new LibraryScreen(newInfo));
        }

        // we re use this so we cant use a subscriber
        class ResultObserver implements Observer<LibraryConnection.Result> {
            final int RETRY_LIMIT = 5;

            LibraryConnection.Result lastResult;
            int retryCount = 0;

            @Override
            public void onCompleted() {
                retryCount = 0;
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "ResultObserver.OnError()");
                if (++retryCount > RETRY_LIMIT) {
                    //TODO show toast
                    return;
                }
                Bundle token = lastResult != null ? lastResult.token : null;
                int backoff = retryCount * 1000;
                if (e instanceof RemoteException) {
                    loadMore(token, backoff);
                } else if (e instanceof LibraryConnection.ResultException) {
                    int code = ((LibraryConnection.ResultException) e).getCode();
                    switch (code) {
                        case OrpheusApi.Error.NETWORK:
                            //TODO
                            break;
                        case OrpheusApi.Error.AUTH_FAILURE:
                            //TODO
                            break;
                        case OrpheusApi.Error.RETRY:
                            loadMore(token, backoff);
                            break;
                        case OrpheusApi.Error.UNKNOWN:
                            loader.connectionManager.onException(loader.libraryInfo.libraryComponent);
                            loadMore(token, backoff);
                        default:
                            break;
                    }
                    return;
                }
                Timber.e(e, "ResultObserver: Unhandled exception");
            }

            @Override
            public void onNext(LibraryConnection.Result result) {
                Timber.v("ResultObserver.onNext() called from:" + Thread.currentThread().getName());
                Timber.v("ResultObserver %s, %s", result.items, result.token);
                lastResult = result;
                onNewResult(result);
            }
        }

    }

}
