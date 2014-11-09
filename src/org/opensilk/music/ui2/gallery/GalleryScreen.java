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

import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui2.BaseSwitcherActivity;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import flow.Layout;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.gallery)
@WithModule(GalleryScreen.Module.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.slide_out_left, R.anim.grow_fade_in }
)
public class GalleryScreen extends Screen {

    @dagger.Module (
            addsTo = BaseSwitcherActivity.Module.class,
            injects = GalleryView.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<GalleryView> {

        final AppPreferences preferences;
        final ActionBarOwner actionBarOwner;

        DelegateActionHandler delegateActionHandler;

        @Inject
        public Presenter(AppPreferences preferences,
                         ActionBarOwner actionBarOwner) {
            Timber.v("new GalleryScreen.Presenter()");
            this.preferences = preferences;
            this.actionBarOwner = actionBarOwner;
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
            // init acitonbar
            updateActionBarWithChildMenuConfig(null);
            // init pager
            List<GalleryPage> galleryPages = preferences.getGalleryPages();
            int startPage = preferences.getInt(AppPreferences.START_PAGE, AppPreferences.DEFAULT_PAGE);
            getView().setup(galleryPages, startPage);
        }

        @Override
        protected void onSave(Bundle outState) {
            Timber.v("onSave(%s)", outState);
            super.onSave(outState);
            if (getView() != null) {
                preferences.putInt(AppPreferences.START_PAGE, getView().viewPager.getCurrentItem());
            }
        }

        void updateActionBarWithChildMenuConfig(ActionBarOwner.MenuConfig menuConfig) {
            if (delegateActionHandler == null) {
                delegateActionHandler = new DelegateActionHandler();
            }
            int[] menus;
            if (menuConfig != null) {
                delegateActionHandler.setDelegate(menuConfig.actionHandler);
                menus = new int[menuConfig.menus.length+2];
                menus[0] = R.menu.shuffle;
                menus[1] = R.menu.search;
                System.arraycopy(menuConfig.menus, 0, menus, 2, menuConfig.menus.length);
            } else {
                delegateActionHandler.setDelegate(null);
                menus = new int[] { R.menu.shuffle, R.menu.search};
            }
            actionBarOwner.setConfig(new ActionBarOwner.Config(true, true, R.string.my_library,
                    new ActionBarOwner.MenuConfig(delegateActionHandler, menus)));
        }

        class DelegateActionHandler implements Func1<Integer, Boolean> {

            Func1<Integer, Boolean> delegate;

            void setDelegate(Func1<Integer, Boolean> delegate) {
                this.delegate = delegate;
            }

            @Override
            public Boolean call(Integer integer) {
                switch (integer) {
                    case R.id.menu_search:
                        return true;
                    case R.id.menu_shuffle:
                        // Starts autoshuffle mode
                        MusicUtils.startPartyShuffle();
                        return true;
                    default:
                        return delegate != null && delegate.call(integer);
                }
            }
        }

    }

}
