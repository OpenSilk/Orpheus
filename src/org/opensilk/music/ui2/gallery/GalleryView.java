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
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.MortarScreen;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;
import org.opensilk.music.widgets.SlidingTabLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.MortarScope;
import mortar.ViewPresenter;

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
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            presenter.dropView(this);
        }
    }

    public void setup(List<Page> pages, int startPage) {
        Adapter adapter = new Adapter(pages);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPage);
        tabBar.setViewPager(viewPager);
    }

    class Adapter extends PagerAdapter {
        private final List<Page> pages;

        public Adapter(Page[] pages) {
            this(Arrays.asList(pages));
        }

        public Adapter(List<Page> pages) {
            this.pages = pages;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Screen screen = pages.get(position).screen;
            // Attach our child screen
            MortarScope newChildScope = presenter.screenScoper.getScreenScope(getContext(), screen);
            // create new scoped context (used to later obtain the child scope)
            Context newChildContext = newChildScope.createContext(getContext());
            // resolve the presenter for the child screen
            ViewPresenter<RecyclerView> childPresenter = obtainPresenter(screen, newChildScope);
            // inflate the recyclerview
            RecyclerView newChild = inflate(newChildContext, R.layout.gallery_recyclerview, container, false);
            // add the new view;
            container.addView(newChild);
            // attach the view to the presenter
            childPresenter.takeView(newChild);
            return newChild;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            RecyclerView oldChild = (RecyclerView) object;
            MortarScope myScope = Mortar.getScope(getContext());
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            ViewPresenter<RecyclerView> oldChildPresenter = obtainPresenter(pages.get(position).screen, oldChildScope);
            //TODO not sure the best order here
            myScope.destroyChild(oldChildScope);
            oldChildPresenter.dropView(oldChild);
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
            return getContext().getString(pages.get(position).titleResource).toUpperCase(Locale.getDefault());
        }


    }

    static <T extends View> T inflate(Context context, int layout, ViewGroup parent, boolean attachToRoot) {
        return (T) LayoutInflater.from(context).inflate(layout, parent, attachToRoot);
    }

    static ViewPresenter<RecyclerView> obtainPresenter(MortarScreen screen, MortarScope scope) {
        Class<?> screenType = ObjectUtils.getClass(screen);
        WithRecyclerViewPresenter withPresenter = screenType.getAnnotation(WithRecyclerViewPresenter.class);
        if (withPresenter == null) {
            throw new IllegalArgumentException("Screen not annotated with @WithPresenter");
        }
        Class<?> presenterClass = withPresenter.value();
        Object presenter = scope.getObjectGraph().get(presenterClass);
        return (ViewPresenter<RecyclerView>) presenter;
    }

}
