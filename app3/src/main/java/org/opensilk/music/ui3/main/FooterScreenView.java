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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.ForegroundLinearLayout;
import org.opensilk.common.ui.widget.ForegroundRelativeLayout;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteResponse;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/15/14.
 */
public class FooterScreenView extends ForegroundRelativeLayout {

    @Inject FooterScreenPresenter presenter;

    @InjectView(R.id.footer_progress) ProgressBar progressBar;
    @InjectView(R.id.footer_pager) ViewPager mViewPager;

    final FooterScreenViewAdapter mAdapter;
    final boolean lightTheme;
    boolean selfChange;

    public FooterScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            FooterScreenComponent component = DaggerService.getDaggerComponent(getContext());
            component.inject(this);
        }
        lightTheme = ThemeUtils.isLightTheme(getContext());
        mAdapter = new FooterScreenViewAdapter(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            ThemeUtils.themeProgressBar(progressBar, R.attr.colorAccent);
            mViewPager.setOffscreenPageLimit(2);
            mViewPager.addOnPageChangeListener(new PageChangeListener());
            mViewPager.setAdapter(mAdapter);
            presenter.takeView(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) presenter.dropView(this);
    }

    FooterScreenViewAdapter getAdapter() {
        return mAdapter;
    }

    void goToCurrent(long qId) {
        int pos = getPosForID(qId);
        if (pos >= 0 && pos != mViewPager.getCurrentItem()) {
            selfChange = true;
            mViewPager.setCurrentItem(pos, true);
        }
    }

    int getPosForID(long qId) {
        List<FooterPageScreen> screens = mAdapter.screens();
        for (FooterPageScreen s : screens) {
            if (s.queueItem.getQueueId() == qId) {
                return screens.indexOf(s);
            }
        }
        return -1;
    }

    class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            if (selfChange) {
                selfChange = false;
                return;
            }
            FooterPageScreen screen = mAdapter.screens().get(position);
            presenter.goToQueueItem(screen.queueItem);
        }
    }

}
