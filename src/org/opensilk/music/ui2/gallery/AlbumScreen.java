/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import com.andrew.apollo.R;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.loader.mediastore.AlbumsLoader;
import org.opensilk.music.ui2.main.God;
import org.opensilk.music.ui2.util.ViewStateSaver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import flow.Layout;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.gallery_album)
public class AlbumScreen implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module (
            addsTo = God.Module.class,
            injects = AlbumView.class
    )
    public static class Module {

    }

    public static class Presenter extends ViewPresenter<AlbumView> {

        final AlbumsLoader loader;
        final Observable<Album> observable;
        final Subscriber<Album> subscriber;
        final Action0 changeListener;
        final ArrayList<Album> list;


        @Inject
        public Presenter(AlbumsLoader loader) {
            this.loader = loader;
            this.observable = loader.getObservable();
            this.subscriber = Subscribers.from(new Observer<Album>() {
                @Override
                public void onCompleted() {
                    AlbumView v = getView();
                    if (v == null) return;
                    v.makeAdapter(list);
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e, "AlbumsLoader");
                }

                @Override
                public void onNext(Album album) {
                    list.add(album);
                }
            });
            this.changeListener = new Action0() {
                @Override
                public void call() {
                    if (getView() != null) subscribe();
                }
            };
            this.list = new ArrayList<>();
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
            loader.registerChangeListener(changeListener);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            loader.unregisterChangeListener(changeListener);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            if (!list.isEmpty()) getView().makeAdapter(list);
            ViewStateSaver.restore(getView(), savedInstanceState, "albumview");
            if (list.isEmpty()) subscribe();
        }

        boolean saved;

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
            if (getView() != null) {
                if (!saved) {
                    ViewStateSaver.save(getView(), outState, "albumview");
                    saved = true;
                }
            }
            subscriber.unsubscribe();
        }

        private void subscribe() {
            list.clear();
            observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber);
        }

        public View.OnClickListener makeOverflowListener(final Context context, final Album album) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            };
        }
    }

}
