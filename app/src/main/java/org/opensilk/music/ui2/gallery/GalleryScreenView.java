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
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.common.widget.SlidingTabLayout;
import org.opensilk.music.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class GalleryScreenView extends LinearLayout {

    @Inject GalleryScreenPresenter presenter;
    @InjectView(R.id.tab_bar) SlidingTabLayout tabBar;
    @InjectView(R.id.pager) ViewPager viewPager;

    public GalleryScreenView(Context context, AttributeSet attrs) {
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
        GalleryScreenAdapter adapter = new GalleryScreenAdapter(getContext(), presenter, galleryPages);
        viewPager.setAdapter(adapter);
        tabBar.setViewPager(viewPager);
        viewPager.setCurrentItem(startPage);
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
