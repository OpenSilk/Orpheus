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

package org.opensilk.music.ui2.details;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.etsy.android.grid.StaggeredGridView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.common.widget.SquareImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.util.PaletteUtil;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.ViewPresenter;

/**
 * Created by drew on 11/8/14.
 */
public class StickyHeaderListView extends FrameLayout {

    @Inject StickyHeaderListPresenter presenter;

    @InjectView(android.R.id.list) AbsListView mList;
    @InjectView(R.id.sticky_header_container) View mStickyHeaderContainer;
    @InjectView(R.id.sticky_header) View mStickyHeader;
    @InjectView(R.id.dummy_header) View mDummyHeader;
    View mListHeader;
    View mHeroContainer;
    //View mListFooter;

    // determines when to animate the dummy views drawable
    private boolean mIsStuck = false;
    // starting color of sticky header
    private int mPreviousPaletteColor;
    // stores sticky header color for save instance
    private int mPaletteColor;

    boolean lightTheme;

    public StickyHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(context, this);
        lightTheme = ThemeUtils.isLightTheme(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mListHeader = ViewUtils.inflate(getContext(), presenter.getListHeaderLayout(), null);
        mHeroContainer = ButterKnife.findById(mListHeader, R.id.hero_container);
        if (mList instanceof ListView) {
            ((ListView) mList).addHeaderView(mListHeader);
//            ((ListView) mList).addFooterView(mListFooter);
        } else if (mList instanceof StaggeredGridView) {
            ((StaggeredGridView) mList).addHeaderView(mListHeader);
//            ((StaggeredGridView) mList).addFooterView(mListFooter);
        } else {
            throw new RuntimeException("List must extend ListView or StaggeredGridView");
        }
        mList.setOnScrollListener(mScrollListener);
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
//        b.putParcelable("superstate", super.onSaveInstanceState());
        b.putBoolean("was_stuck", mIsStuck);
        b.putInt("palette_color", mPaletteColor);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle b = (Bundle) state;
//        super.onRestoreInstanceState(b.getParcelable("superstate"));
        mIsStuck = b.getBoolean("was_stuck", false);
        mPreviousPaletteColor = mPaletteColor = b.getInt("palette_color");
    }

    public <T extends AbsListView> T getListView() {
        return (T) mList;
    }

    public PaletteObserver getPaletteObserver() {
        return mPaletteObserver;
    }

    private ValueAnimator makeSlideAnimator(int start, int end, final ClipDrawable drawable) {
        final ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final int value = (Integer) animation.getAnimatedValue();
                if (drawable != null) {
                    drawable.setLevel(value);
                }
            }
        });
        return animator;
    }

    Drawable originalBackground;
    PaletteObserver mPaletteObserver = new PaletteObserver() {
        @Override
        public void onNext(PaletteResponse paletteResponse) {
            Palette palette = paletteResponse.palette;
            Palette.Swatch swatch = lightTheme ? palette.getLightMutedSwatch() : palette.getDarkMutedSwatch();
            if (swatch == null) swatch = palette.getMutedSwatch();
            if (swatch != null) {
                final ClipDrawable dummyBackground =
                        new ClipDrawable(new ColorDrawable(mPaletteColor), Gravity.BOTTOM, ClipDrawable.VERTICAL);
                if (mIsStuck) {
                    dummyBackground.setLevel(10000);
                } else {
                    dummyBackground.setLevel(0);
                }
                mDummyHeader.setBackgroundDrawable(dummyBackground);
                if (paletteResponse.shouldAnimate) {
                    originalBackground = mStickyHeader.getBackground();
                    Drawable d = mStickyHeader.getBackground();
                    Drawable d2 = new ColorDrawable(swatch.getRgb());
                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{d,d2});
                    td.setCrossFadeEnabled(true);
                    mStickyHeader.setBackgroundDrawable(td);
                    td.startTransition(SquareImageView.TRANSITION_DURATION);
                } else {
                    originalBackground = mStickyHeader.getBackground();
                    mStickyHeader.setBackgroundColor(swatch.getRgb());
                }
            }
        }
    };

    private final AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            // logic here derived from http://antoine-merle.com/blog/2013/10/04/making-that-google-plus-profile-screen/
            if (visibleItemCount == 0) return;
            if (firstVisibleItem == 0) {
                // parallax
                mHeroContainer.setTranslationY(-mList.getChildAt(0).getTop() / 2);
            }
            // sticky header
            final int top = mListHeader.getTop();
            final int stickyHeight = mStickyHeaderContainer.getMeasuredHeight();
            final int headerHeight = mListHeader.getMeasuredHeight();
            final int delta = headerHeight - stickyHeight;
            final int pos = delta + top;
            // reposition header
            mStickyHeaderContainer.setTranslationY(Math.max(pos,0));
            if (pos < 0 && !mIsStuck) {
                mIsStuck = true;
                makeSlideAnimator(0, 10000, (ClipDrawable) mDummyHeader.getBackground()).start();
            } else if (pos > 0 && mIsStuck) {
                mIsStuck = false;
                makeSlideAnimator(10000, 0, (ClipDrawable) mDummyHeader.getBackground()).start();
            }
        }
    };

}
