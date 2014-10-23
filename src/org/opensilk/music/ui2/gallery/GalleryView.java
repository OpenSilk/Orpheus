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

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.MortarScreen;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.util.ViewStateSaver;
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

    public void setup(List<GalleryPage> galleryPages, int startPage) {
        viewPager.setOffscreenPageLimit(galleryPages.size());
        Adapter adapter = new Adapter(galleryPages);
        viewPager.setAdapter(adapter);
        tabBar.setViewPager(viewPager);
        viewPager.setCurrentItem(startPage);
    }

    class Adapter extends PagerAdapter {
        private final List<GalleryPage> galleryPages;
        Object mCurrentPrimaryItem;

        public Adapter(GalleryPage[] galleryPages) {
            this(Arrays.asList(galleryPages));
        }

        public Adapter(List<GalleryPage> galleryPages) {
            this.galleryPages = galleryPages;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Screen screen = galleryPages.get(position).screen;
            // Attach our child screen
            MortarScope newChildScope = presenter.screenScoper.getScreenScope(getContext(), screen);
            Timber.i("instatiateItem %s", newChildScope.getName());
            // create new scoped context (used to later obtain the child scope)
            Context newChildContext = newChildScope.createContext(getContext());
            // resolve the presenter for the child screen
            final ViewPresenter<GalleryPageView> childPresenter = obtainPresenter(screen, newChildScope);
            // inflate the recyclerview
            final GalleryPageView newChild = ViewStateSaver.inflate(newChildContext, R.layout.gallery_page, container);
            // set the presenter
            newChild.setPresenter(childPresenter);
            // add the new view;
            container.addView(newChild);
            return newChild;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // cast
            GalleryPageView oldChild = (GalleryPageView) object;
            // retrieve our scope
            MortarScope myScope = Mortar.getScope(getContext());
            // retrieve child scope
            MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
            Timber.i("destroyItem %s", oldChildScope.getName());
            //TODO not sure the best order here
            // destroy the child
            myScope.destroyChild(oldChildScope);
            container.removeView(oldChild);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (object != mCurrentPrimaryItem) {
                GalleryPageView currentChild = (GalleryPageView) object;
                ViewPresenter<GalleryPageView> childPresenter = currentChild.getPresenter();
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
            return view.equals(o);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getContext().getString(galleryPages.get(position).titleResource).toUpperCase(Locale.getDefault());
        }

    }

    //TODO cache these
    static ViewPresenter<GalleryPageView> obtainPresenter(MortarScreen screen, MortarScope scope) {
        Class<?> screenType = ObjectUtils.getClass(screen);
        WithGalleryPageViewPresenter withPresenter = screenType.getAnnotation(WithGalleryPageViewPresenter.class);
        if (withPresenter == null) {
            throw new IllegalArgumentException("Screen not annotated with @WithPresenter");
        }
        Class<?> presenterClass = withPresenter.value();
        Object presenter = scope.getObjectGraph().get(presenterClass);
        return (ViewPresenter<GalleryPageView>) presenter;
    }

}
