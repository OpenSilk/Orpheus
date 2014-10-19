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

import android.os.Bundle;
import android.view.View;

import org.opensilk.common.mortar.ScreenScoper;
import org.opensilk.music.R;

import org.opensilk.music.ui2.main.MainScreen;
import org.opensilk.music.ui2.util.ViewStateSaver;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.Blueprint;
import mortar.MortarScope;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.gallery_pager)
public class GalleryScreen implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module (
            addsTo = MainScreen.Module.class,
            injects = {
                    GalleryView.class,
            }
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<GalleryView> {

        final ScreenScoper screenScoper;

        @Inject
        public Presenter(ScreenScoper screenScoper) {
            Timber.v("new GalleryScreen.Presenter()");
            this.screenScoper = screenScoper;
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

            getView().setup(Arrays.asList(Page.values()), 4);
//            ViewStateSaver.restore(getView(), savedInstanceState, "pager");
        }

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
//            if (getView() != null) {
//                ViewStateSaver.save(getView(), outState, "pager");
//            }
        }

    }

}
