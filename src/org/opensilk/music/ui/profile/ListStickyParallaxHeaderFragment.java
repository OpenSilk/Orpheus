/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.profile;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.SquareImageView;
import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;
import com.etsy.android.grid.StaggeredGridView;

import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.util.PaletteUtil;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import butterknife.ButterKnife;

/**
 * Detail view that mimics the io2014 app
 * layout consists of a hero image, sticky header, and staggered grid (or list) view
 * the hero is parallax(ed) on scroll of the grid/list view, once the header at the
 * base of the hero image reaches the top it sticks, and the background expands
 * to fill in the action bar area
 *
 * Created by drew on 7/9/14.
 */
public class ListStickyParallaxHeaderFragment extends Fragment {

    protected View mListHeader;
    protected FrameLayout mHeroContainer;
    protected AbsListView mList;
    protected View mStickyHeaderContainer;
    protected View mStickyHeader;
    protected View mHeaderDummy;
    protected View mListFooter;

    // determines when to animate the dummy views drawable
    private boolean mIsStuck = false;

    boolean mLightTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsStuck = savedInstanceState.getBoolean("was_stuck");
        }
        mLightTheme = ThemeUtils.isLightTheme(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // container holding gridview and stickyheader
        View frame = inflater.inflate(getListLayout(), container, false);
        // gridview
        mList = ButterKnife.findById(frame, android.R.id.list);
        // stickyheader container
        mStickyHeaderContainer = ButterKnife.findById(frame, R.id.sticky_header_container);
        // stickyheader
        mStickyHeader = ButterKnife.findById(mStickyHeaderContainer, R.id.sticky_header);
        // dummy view inside header to prevent real header from scrolling under the actionbar
        mHeaderDummy = ButterKnife.findById(mStickyHeaderContainer, R.id.dummy);
        // grid header container
        mListHeader = inflater.inflate(getHeaderLayout(), null/*null or listview explodes*/, false);
        // hero container containing images
        mHeroContainer = ButterKnife.findById(mListHeader, R.id.hero_container);
        // list footer
        mListFooter = inflater.inflate(R.layout.list_footer, null, false);
        return frame;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
        //setup the dummy header background with the same color as the stickyheader
        final ClipDrawable dummyBackground = new ClipDrawable(mStickyHeader.getBackground(), Gravity.BOTTOM, ClipDrawable.VERTICAL);
        dummyBackground.setLevel(mIsStuck ? 10000 : 0);
        mHeaderDummy.setBackgroundDrawable(dummyBackground);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("was_stuck", mIsStuck);
    }

    protected int getHeaderLayout() {
        return R.layout.profile_hero_header;
    }

    protected int getListLayout() {
        return R.layout.profile_staggeredgrid_frame;
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

    protected final PaletteObserver mPaletteObserver = new PaletteObserver() {
        @Override
        public void onNext(PaletteResponse paletteResponse) {
            Palette palette = paletteResponse.palette;
            Palette.Swatch swatch = mLightTheme ? palette.getLightMutedSwatch() : palette.getDarkMutedSwatch();
            if (swatch == null) swatch = palette.getMutedSwatch();
            if (swatch != null) {
                final ClipDrawable dummyBackground =
                        new ClipDrawable(new ColorDrawable(swatch.getRgb()), Gravity.BOTTOM, ClipDrawable.VERTICAL);
                dummyBackground.setLevel(mIsStuck ? 10000 : 0);
                mHeaderDummy.setBackgroundDrawable(dummyBackground);
                if (paletteResponse.shouldAnimate) {
                    final Drawable d = mStickyHeader.getBackground();
                    final Drawable d2 = new ColorDrawable(swatch.getRgb());
                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{d,d2});
                    td.setCrossFadeEnabled(true);
                    mStickyHeader.setBackgroundDrawable(td);
                    td.startTransition(SquareImageView.TRANSITION_DURATION);
                } else {
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
            if (firstVisibleItem == 0 && mList.getChildCount() > 0) {
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
                makeSlideAnimator(0, 10000, (ClipDrawable)mHeaderDummy.getBackground()).start();
            } else if (pos > 0 && mIsStuck) {
                mIsStuck = false;
                makeSlideAnimator(10000, 0, (ClipDrawable)mHeaderDummy.getBackground()).start();
            }
        }
    };

}
