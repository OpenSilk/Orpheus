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

package org.opensilk.music.ui3.profile.bio;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.loader.TypedBundleableLoader;
import org.opensilk.music.model.Model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.ViewPresenter;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by drew on 10/18/15.
 */
@ScreenScope
public class BioScreenPresenter extends ViewPresenter<BioScreenView> {

    Subscription contentSubscription;
    final Context appContext;
    final BioSummary bioSummary;
    final ArrayList<Model> models = new ArrayList<>();

    @Inject
    public BioScreenPresenter(
            @ForApplication Context appContext,
            BioSummary bioSummary
    ) {
        this.bioSummary = bioSummary;
        this.appContext = appContext;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (RxUtils.isSubscribed(contentSubscription)) {
            if (!models.isEmpty()) {
                getView().onModels(models);
            }
        } else {
            contentSubscription = TypedBundleableLoader.<Model>create(appContext)
                    .setMethod(LibraryMethods.LIST)
                    .setUri(bioSummary.getUri())
                    .createObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Model>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        @DebugLog
                        public void onNext(List<Model> models) {
                            onModels(models);
                        }
                    });
        }
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (RxUtils.isSubscribed(contentSubscription)) {
            contentSubscription.unsubscribe();
        }
    }

    void onModels(List<Model> models) {
        this.models.addAll(models);
        if (hasView()) {
            getView().onModels(models);
        }
    }

    public ActionBarConfig getActionBarConfig() {
        return ActionBarConfig.builder()
                .setTitle(bioSummary.getName())
                .build();
    }
}
