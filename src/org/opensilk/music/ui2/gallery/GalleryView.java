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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.music.R;
import org.opensilk.music.widgets.SlidingTabLayout;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 10/3/14.
 */
public class GalleryView extends LinearLayout {

    @Inject
    GalleryPresenter presenter;
    @InjectView(R.id.tab_bar)
    SlidingTabLayout tabBar;
    @InjectView(R.id.pager)
    ViewPager viewPager;

    public GalleryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
        ButterKnife.inject(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void setup(List<Page> pages, int startPage) {
        Adapter adapter = new Adapter(getContext(), pages);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPage);
        tabBar.setViewPager(viewPager);
    }

    public static class Adapter extends PagerAdapter {
        private final Context context;
        private final List<Page> pages;

        public Adapter(Context context, Page[] pages) {
            this(context, Arrays.asList(pages));
        }

        public Adapter(Context context, List<Page> pages) {
            this.context = context;
            this.pages = pages;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Blueprint screen = initScreen(pages.get(position).clazz);
            MortarScope myScope = Mortar.getScope(context);
            MortarScope newChildScope = myScope.requireChild(screen);
            Context childContext = newChildScope.createContext(context);
            android.view.View newChild = Layouts.createView(childContext, screen);
            container.addView(newChild);
            return newChild;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            android.view.View oldChild = (android.view.View) object;
            MortarScope myScope = Mortar.getScope(context);
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            myScope.destroyChild(oldChildScope);
            container.removeView(oldChild);
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public boolean isViewFromObject(android.view.View view, Object o) {
            return view.equals(o);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return context.getString(pages.get(position).titleResource);
        }

        private static <T extends Blueprint> T initScreen(Class<T> clazz) throws IllegalArgumentException {
            try {
                return clazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

}
