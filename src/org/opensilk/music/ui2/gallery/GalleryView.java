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
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.mortarflow.MortarPagerAdapter;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.common.widget.SlidingTabLayout;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Layout;
import mortar.Mortar;
import mortar.ViewPresenter;
import timber.log.Timber;

import static org.opensilk.common.util.Preconditions.checkNotNull;

/**
 * Created by drew on 10/3/14.
 */
public class GalleryView extends LinearLayout {

    @Inject GalleryScreen.Presenter presenter;
    @InjectView(R.id.tab_bar) SlidingTabLayout tabBar;
    @InjectView(R.id.pager) ViewPager viewPager;

    public GalleryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            Mortar.inject(context, this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            presenter.takeView(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
//        Timber.v("onDetachedFromWindow()");
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            presenter.dropView(this);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
//        Timber.v("onSaveInstanceState");
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
//        Timber.v("onRestoreInstanceState");
        super.onRestoreInstanceState(state);
    }

    public void setup(List<GalleryPage> galleryPages, int startPage) {
        Adapter adapter = new Adapter(getContext(), presenter, galleryPages);
        viewPager.setAdapter(adapter);
        tabBar.setViewPager(viewPager);
        viewPager.setCurrentItem(startPage);
    }

    static class Adapter extends MortarPagerAdapter<Screen, GalleryPageView> {
        private Object mCurrentPrimaryItem;
        private GalleryScreen.Presenter presenter;

        Adapter(Context context, GalleryScreen.Presenter presenter, List<GalleryPage> pages) {
            super(context, extractScreens(pages));
            this.presenter = presenter;
        }

        static List<Screen> extractScreens(List<GalleryPage> pages) {
            List<Screen> sceens = new ArrayList<>(pages.size());
            for (GalleryPage p : pages) {
                sceens.add(p.screen);
            }
            return sceens;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (object != mCurrentPrimaryItem) {
                Page currentPage = (Page) object;
                ViewPresenter<GalleryPageView> childPresenter = (currentPage.view).getPresenter();
                ActionBarOwner.MenuConfig menuConfig = null;
                if (childPresenter != null && childPresenter instanceof HasOptionsMenu) {
                    menuConfig = ((HasOptionsMenu) childPresenter).getMenuConfig();
                }
                presenter.updateActionBarWithChildMenuConfig(menuConfig);
                mCurrentPrimaryItem = object;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Screen screen = screens.get(position);
            int res = getGalleryPageTitleResource(screen);
            return context.getString(res).toUpperCase(Locale.getDefault());
        }

    }

    private static final Map<Class, Integer> PAGE_TITLES = new HashMap<>(GalleryPage.values().length);
    public static int getGalleryPageTitleResource(Screen s) {
        Class<? extends Screen> screenType = ObjectUtils.getClass(s);
        Integer titleRes = PAGE_TITLES.get(screenType);
        if (titleRes == null) {
            GalleryPageTitle pageTitle = screenType.getAnnotation(GalleryPageTitle.class);
            titleRes = pageTitle.value();
            PAGE_TITLES.put(screenType, titleRes);
        }
        return titleRes;
    }

}
