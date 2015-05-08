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

package org.opensilk.music.ui3.gallery;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

/**
 * Created by drew on 4/20/15.
 */
@GalleryScreenScope
public class GalleryScreenPresenter extends ViewPresenter<GalleryScreenView> {

    final AppPreferences preferences;
    final GalleryScreen screen;

    DelegateActionHandler delegateActionHandler;

    @Inject
    public GalleryScreenPresenter(
            AppPreferences preferences,
            GalleryScreen screen
    ) {
        Timber.v("new GalleryScreen.Presenter()");
        this.preferences = preferences;
        this.screen = screen;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
//            Timber.v("onEnterScope()");
        super.onEnterScope(scope);
    }

    @Override
    protected void onExitScope() {
//            Timber.v("onExitScope()");
        super.onExitScope();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
//            Timber.v("onLoad(%s)", savedInstanceState);
        super.onLoad(savedInstanceState);
        // init acitonbar
        updateActionBarWithChildMenuConfig(null);
        // init pager
//        List<GalleryPage> galleryPages = preferences.getGalleryPages();
//        int startPage = preferences.getInt(AppPreferences.START_PAGE, AppPreferences.DEFAULT_PAGE);
//        getView().setup(galleryPages, startPage);
        getView().setup(screen.pages, 0);
    }

    @Override
    protected void onSave(Bundle outState) {
//            Timber.v("onSave(%s)", outState);
        super.onSave(outState);
        if (getView() != null) {
//            preferences.putInt(AppPreferences.START_PAGE, getView().mViewPager.getCurrentItem());
        }
    }

    void updateActionBarWithChildMenuConfig(ActionBarMenuConfig menuConfig) {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }
        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        builder.setActionHandler(delegateActionHandler);
        builder.withMenu(R.menu.shuffle);

        if (menuConfig != null) {
            delegateActionHandler.setDelegate(menuConfig.actionHandler);
            if (menuConfig.menus.length > 0) {
                builder.withMenus(menuConfig.menus);
            }
        } else {
            delegateActionHandler.setDelegate(null);
        }

//        actionBarOwner.setConfig(new ActionBarOwner.Config.Builder()
//                .setTitle(R.string.my_library)
//                .setMenuConfig(builder.build())
//                .build());
    }

    class DelegateActionHandler implements Func2<Context, Integer, Boolean> {

        Func2<Context, Integer, Boolean> delegate;

        void setDelegate(Func2<Context, Integer, Boolean> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Boolean call(Context context, Integer integer) {
            switch (integer) {
                case R.id.menu_shuffle:
//                    musicService.startPartyShuffle();
                    return true;
                default:
                    return delegate != null && delegate.call(context, integer);
            }
        }
    }

}
