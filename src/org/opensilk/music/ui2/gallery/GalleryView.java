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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.common.widget.SlidingTabLayout;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
public class GalleryView extends LinearLayout {

    @Inject
    GalleryScreen.Presenter presenter;
    @InjectView(R.id.tab_bar)
    SlidingTabLayout tabBar;
    @InjectView(R.id.pager)
    ViewPager viewPager;

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
        Timber.v("onDetachedFromWindow()");
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            presenter.dropView(this);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Timber.v("onSaveInstanceState");
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Timber.v("onRestoreInstanceState");
        super.onRestoreInstanceState(state);
    }


    public void setup(List<GalleryPage> galleryPages, int startPage) {
        Adapter adapter = new Adapter(galleryPages);
        viewPager.setAdapter(adapter);
        tabBar.setViewPager(viewPager);
        viewPager.setCurrentItem(startPage);
    }

    class Adapter extends PagerAdapter {
        private final MortarContextFactory contextFactory = new MortarContextFactory();
        private final List<GalleryPage> galleryPages;
        private final Set<Page> activePages;
        private Bundle savedState;
        private Object mCurrentPrimaryItem;

        private class Page {
            Screen screen;
            GalleryPageView view;
            private Page(Screen screen, GalleryPageView view) {
                this.screen = screen;
                this.view = view;
            }
        }

        public Adapter(GalleryPage[] galleryPages) {
            this(Arrays.asList(galleryPages));
        }

        public Adapter(List<GalleryPage> galleryPages) {
            this.galleryPages = galleryPages;
            this.activePages = new LinkedHashSet<>(galleryPages.size());
            this.savedState = new Bundle(galleryPages.size());
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            GalleryPage page = galleryPages.get(position);
            Screen screen = page.screen;
            Timber.v("instantiateItem %s", screen.getName());
            Context newChildContext = contextFactory.setUpContext(screen, getContext());
            final GalleryPageView newChild = ViewUtils.inflate(newChildContext, R.layout.gallery_page, container, false);
            ViewUtils.restoreState(newChild, savedState, screen.getName());
            container.addView(newChild);
            Page newPage = new Page(screen, newChild);
            activePages.add(newPage);
            return newPage;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Page oldPage = (Page) object;
            Timber.v("destroyItem %s", oldPage.screen.getName());
            activePages.remove(oldPage);
            ViewUtils.saveState(oldPage.view, savedState, oldPage.screen.getName());
            contextFactory.tearDownContext(oldPage.view.getContext());
            container.removeView(oldPage.view);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (object != mCurrentPrimaryItem) {
                Page currentPage = (Page) object;
                ViewPresenter<GalleryPageView> childPresenter = currentPage.view.getPresenter();
                ActionBarOwner.MenuConfig menuConfig = null;
                if (childPresenter != null && childPresenter instanceof HasOptionsMenu) {
                    menuConfig = ((HasOptionsMenu) childPresenter).getMenuConfig();
                }
                presenter.updateActionBarWithChildMenuConfig(menuConfig);
                mCurrentPrimaryItem = object;
            }
        }

        @Override
        public int getCount() {
            return galleryPages.size();
        }

        @Override
        public boolean isViewFromObject(android.view.View view, Object o) {
            return view.equals(((Page) o).view);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getContext().getString(galleryPages.get(position).titleResource).toUpperCase(Locale.getDefault());
        }

        @Override
        public Parcelable saveState() {
            for (Page p : activePages) {
                ViewUtils.saveState(p.view, savedState, p.screen.getName());
            }
            return savedState;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            if (state == null || !(state instanceof Bundle)) return;
            Bundle b = (Bundle) state;
            b.setClassLoader(loader);
            savedState = b;
        }
    }

}
