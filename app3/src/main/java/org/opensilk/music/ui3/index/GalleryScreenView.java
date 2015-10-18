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

package org.opensilk.music.ui3.index;

import android.content.Context;
import android.os.Parcelable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
public class GalleryScreenView extends CoordinatorLayout {

    @Inject GalleryScreenPresenter mPresenter;
    @Inject ToolbarOwner mToolbarOwner;

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.tab_bar) TabLayout mTabBar;
    @InjectView(R.id.pager) ViewPager mViewPager;

    public GalleryScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        GalleryScreenComponent component = DaggerService.getDaggerComponent(context);
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mToolbarOwner.attachToolbar(mToolbar);
            mToolbarOwner.setConfig(mPresenter.getActionBarConfig());
            mPresenter.takeView(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        Timber.v("onDetachedFromWindow()");
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            mToolbarOwner.detachToolbar(mToolbar);
            mPresenter.dropView(this);
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

    public void setup(List<GalleryPage> pages, int startPage) {
        GalleryScreenViewAdapter adapter = new GalleryScreenViewAdapter(
                //pages expect to descend from the activity context
                UtilsCommon.findActivity(getContext()), mPresenter, pages);
        mViewPager.setAdapter(adapter);
        mTabBar.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabBar.setTabTextColors(ContextCompat.getColor(getContext(), R.color.white),
                ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorAccent));
        mTabBar.setupWithViewPager(mViewPager);
        mViewPager.setCurrentItem(startPage);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mPresenter.actionModePresenter.cancelActionMode();
            }
        });
    }

    public void updateToolbar() {
        if (isAttachedToWindow()) {
            mToolbarOwner.setConfig(mPresenter.getActionBarConfig());
        }
    }

}
