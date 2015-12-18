/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.ObjectUtils;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.R;
import org.opensilk.music.library.client.TypedBundleableLoader;
import org.opensilk.music.library.playlist.PlaylistManager;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 12/17/15.
 */
public class PlaylistProgressScreenPresenter extends Presenter<PlaylistProgressScreenFragment> {

    final Context appContext;
    final PlaylistManager manager;
    final PlaylistProgressScreen.Operation operation;
    final Bundle extras;
    final ActivityResultsController activityResultsController;

    enum State {
        NONE,
        RUNNING,
        COMPLETE,
        ERROR
    }

    State state = State.NONE;
    Subscription subscription = null;

    int numDeleted = 0;
    Playlist moddedPlaylist;

    @Inject
    public PlaylistProgressScreenPresenter(
            @ForApplication Context context,
            PlaylistProgressScreen screen,
            ActivityResultsController activityResultsController
    ) {
        this.appContext = context;
        this.manager = new PlaylistManager(context, BundleHelper.getString(screen.extras));
        this.operation = screen.operation;
        this.extras = screen.extras;
        this.activityResultsController = activityResultsController;
    }

    @Override
    protected BundleService extractBundleService(PlaylistProgressScreenFragment view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        switch (state) {
            case NONE:
                doOperation();
                break;
            case RUNNING:
                //pass
                break;
            case COMPLETE:
            case ERROR:
                onDone();
                break;
        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        RxUtils.unsubscribe(subscription);
    }

    void doOperation() {
        state = State.RUNNING;
        switch (operation) {
            case CREATE:
                doCreate();
                break;
            case ADDTO:
                doAddTo();
                break;
            case DELETE:
                doDelete();
                break;
            case UPDATE:
                doUpdate();
                break;
        }
    }

    void doCreate() {
        final String name = BundleHelper.getString2(extras);
        subscription = manager.create(name)
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Uri>() {
                    @Override
                    public void onCompleted() {
                        state = State.COMPLETE;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.w(e, "Create playlist");
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(Uri uri) {
                        //pass;
                    }
                });
    }

    void doAddTo() {
        final Uri playlist = BundleHelper.getUri(extras);
        int listKind = BundleHelper.getInt2(extras);
        List<Uri> uris = BundleHelper.getList(extras);
        Observable<Playlist> o = null;
        if (listKind == PlaylistChooseScreen.ListKind.POINTER) {
            Observable<Observable<List<Track>>> loaderCreator = Observable.from(uris)
                    .map(new Func1<Uri, Observable<List<Track>>>() {
                        @Override
                        public Observable<List<Track>> call(final Uri uri) {
                            //Use defer for lazy creation
                            return Observable.defer(new Func0<Observable<List<Track>>>() {
                                @Override
                                public Observable<List<Track>> call() {
                                    return TypedBundleableLoader.<Track>create(appContext)
                                            .setUri(uri).createObservable();
                                }
                            });
                        }
                    });
            o = Observable.mergeDelayError(loaderCreator, 5)
                    .subscribeOn(Schedulers.computation())
                    .collect(new Func0<List<Uri>>() {
                        @Override
                        public List<Uri> call() {
                            return new ArrayList<Uri>();
                        }
                    }, new Action2<List<Uri>, List<Track>>() {
                        @Override
                        public void call(List<Uri> uris, List<Track> tracks) {
                            for (Track track : tracks) {
                                uris.add(track.getUri());
                            }
                        }
                    })
                    .flatMap(new Func1<List<Uri>, Observable<Playlist>>() {
                        @Override
                        public Observable<Playlist> call(List<Uri> uris) {
                            return manager.addTo(playlist, uris);
                        }
                    });
        } else {
            o = manager.addTo(playlist, uris)
                    .subscribeOn(Schedulers.computation());
        }
        subscription = o
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Playlist>() {
                    @Override
                    public void onCompleted() {
                        state = State.COMPLETE;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.w(e, "Add to playlist");
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(Playlist playlist) {
                        moddedPlaylist = playlist;
                    }
                });
    }

    void doDelete() {
        List<Uri> playlists = BundleHelper.getList(extras);
        subscription = manager.delete(playlists)
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        state = State.COMPLETE;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.w(e, "Delete playlists");
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(Integer integer) {
                        numDeleted = integer;
                    }
                });
    }

    void doUpdate() {
        Uri playlist = BundleHelper.getUri(extras);
        List<Uri> uris = BundleHelper.getList(extras);
        subscription = manager.update(playlist, uris)

                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Playlist>() {
                    @Override
                    public void onCompleted() {
                        state = State.COMPLETE;
                        onDone();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.w(e, "Update playlist");
                        state = State.ERROR;
                        onDone();
                    }

                    @Override
                    public void onNext(Playlist playlist) {
                        moddedPlaylist = playlist;
                    }
                });
    }

    void onDone() {
        if (!hasView()) return;
        switch (state) {
            case COMPLETE: {
                switch (operation) {
                    case ADDTO: {
                        Intent intent = new Intent();
                        if (moddedPlaylist != null) {
                            intent.putExtra("plist", moddedPlaylist.toBundle());
                        }
                        activityResultsController.setResultAndFinish(Activity.RESULT_OK, intent);
                        break;
                    }
                    case DELETE: {
                        String text = getView().getResources().getQuantityString(R.plurals.NNNitemsdeleted, numDeleted, numDeleted);
                        Toast.makeText(getView().getActivity(), text, Toast.LENGTH_SHORT).show();
                        getView().dismiss();
                        break;
                    }
                    default:
                        getView().dismiss();
                }
                break;
            }
            case ERROR: {
                Toast.makeText(getView().getActivity(), R.string.err_generic, Toast.LENGTH_SHORT).show();
                switch (operation) {
                    case ADDTO: {
                        Intent intent = new Intent();
                        activityResultsController.setResultAndFinish(Activity.RESULT_CANCELED, intent);
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

    static <T> Observable<T> addDelay(Observable<T> o, final long startTime) {
        return o.delay(new Func1<T, Observable<Long>>() {
            @Override
            public Observable<Long> call(T t) {
                long now = System.currentTimeMillis();
                if (now - startTime > 1000) {
                    return Observable.just(1L);
                } else {
                    Timber.d("Delaying %dms", now - startTime);
                    return Observable.timer(now - startTime, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

}
