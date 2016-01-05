/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui3.library;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.opensilk.bundleable.BadBundleableException;
import org.opensilk.bundleable.BundleableUtil;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.R;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.client.LibraryClient;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.model.Container;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 1/4/16.
 */
@ScreenScope
public class LibraryOpScreenPresenter extends Presenter<LibraryOpScreenFragment> {

    private enum State {
        PENDING,
        WORKING,
        ERROR,
        SUCCESS,
    }

    static final long DELAY = 400;

    final LibraryOpScreen screen;
    final IndexClient indexClient;
    final Context appContext;
    final FragmentManagerOwner fm;

    State state = State.PENDING;
    Subscription subscription;

    Container fetchedContainer;

    @Inject
    public LibraryOpScreenPresenter(
            LibraryOpScreen screen,
            IndexClient indexClient,
            @ForApplication Context context,
            FragmentManagerOwner fm
    ) {
        this.screen = screen;
        this.indexClient = indexClient;
        this.appContext = context;
        this.fm = fm;
    }

    @Override
    protected BundleService extractBundleService(LibraryOpScreenFragment view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        switch (state) {
            case PENDING:
                doOp();
                break;
            case WORKING:
                //pass
                break;
            case ERROR:
            case SUCCESS:
                onDone();
                break;
        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        RxUtils.unsubscribe(subscription);
    }

    void doOp() {
        switch (screen.op) {
            case UNINDEX:
                unIndexContainers();
                break;
            case DELETE:
                deleteItems();
                break;
            case GET_CONTAINER:
                getContainer();
                break;
        }
    }

    void unIndexContainers() {
        final List<Bundle> containerBundles = BundleHelper.getList(screen.extras);
        subscription = Observable.from(containerBundles)
                .map(new Func1<Bundle, Container>() {
                    @Override
                    public Container call(Bundle bundle) {
                        try {
                            return (Container) BundleableUtil.materializeBundle(bundle);
                        } catch (BadBundleableException e) {
                            throw Exceptions.propagate(e);
                        }
                    }
                })
                .map(new Func1<Container, Boolean>() {
                    @Override
                    public Boolean call(Container container) {
                        return indexClient.remove(container);
                    }
                })
                .subscribeOn(Schedulers.io())
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        state = State.SUCCESS;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        //pass
                    }
                });
    }

    void deleteItems() {
        List<Uri> uris = BundleHelper.getList(screen.extras);
        if (uris == null || uris.isEmpty()) {
            onDone();
            return;
        }
        Uri notifyUri = BundleHelper.getUri(screen.extras);

        subscription = indexClient.deleteItems(uris, notifyUri)
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Uri>>() {
                    boolean hadNext = false;

                    @Override
                    public void onCompleted() {
                        if (hadNext) {
                            indexClient.rescan();
                        }
                        state = State.SUCCESS;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(List<Uri> uris) {
                        hadNext = true;
                    }
                });
    }

    void getContainer() {
        Uri uri = BundleHelper.getUri(screen.extras);
        subscription = TypedBundleableLoader.<Container>create(appContext)
                .setUri(uri)
                .setMethod(LibraryMethods.GET)
                .createObservable()
                .delay(DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Container>>() {
                    @Override
                    public void onCompleted() {
                        state = fetchedContainer != null ? State.SUCCESS : State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(List<Container> containers) {
                        if (containers.size() == 1) {
                            fetchedContainer = containers.get(0);
                        }
                    }
                });
    }

    void onDone() {
        if (!hasView()) return;
        switch (state) {
            case ERROR: {
                Toast.makeText(getView().getActivity(), R.string.err_generic, Toast.LENGTH_LONG).show();
                getView().dismiss();
                break;
            }
            case SUCCESS:
            default: {
                switch (screen.op) {
                    case GET_CONTAINER: {
                        if (fetchedContainer != null) {
                            LibraryConfig config = LibraryClient.create(appContext, fetchedContainer.getUri()).getConfig();
                            if (config != null) {
                                fm.replaceMainContent(FoldersScreenFragment.ni(appContext, config, fetchedContainer), true);
                            }
                        }
                        getView().dismiss();
                        break;
                    }
                    default:
                        getView().dismiss();
                        break;
                }
                break;
            }
        }
    }
}
