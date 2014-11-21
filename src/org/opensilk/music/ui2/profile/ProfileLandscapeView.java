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

package org.opensilk.music.ui2.profile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.SquareImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.Mortar;

/**
 * Created by drew on 11/21/14.
 */
public class ProfileLandscapeView extends LinearLayout {

    @Inject BasePresenter<ProfileLandscapeView> presenter;

    @InjectView(android.R.id.list) AbsListView mList;
    @InjectView(R.id.sticky_header_container) @Optional View mStickyHeaderContainer;
    @InjectView(R.id.sticky_header) ViewGroup mStickyHeader;
    @InjectView(R.id.dummy) @Optional View mHeaderDummy;
    @InjectView(R.id.info_title) TextView mTitle;
    @InjectView(R.id.info_subtitle) TextView mSubtitle;
    View mListHeader;
    FrameLayout mHeroContainer;
    AnimatedImageView mArtwork;
    AnimatedImageView mArtwork2;
    AnimatedImageView mArtwork3;
    AnimatedImageView mArtwork4;

    boolean mLightTheme;
    boolean mIsStuck;

    public ProfileLandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
        mLightTheme = ThemeUtils.isLightTheme(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        int numArtwork = presenter.getNumArtwork();
        int headerlayout;
        if (numArtwork >= 4) {
            headerlayout = R.layout.profile_hero4;
        } else if (numArtwork >= 2) {
            headerlayout = R.layout.profile_hero2;
        } else {
            headerlayout = R.layout.profile_hero;
        }
        mListHeader = LayoutInflater.from(getContext()).inflate(headerlayout, null);
        mHeroContainer = ButterKnife.findById(mListHeader, R.id.hero_container);
        mArtwork4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
        mArtwork3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
        mArtwork2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
        mArtwork = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mStickyHeaderContainer != null) {
            // portrait mode header gets added to list
            if (mList instanceof ListView) {
                ((ListView) mList).addHeaderView(mListHeader);
            } else if (mList instanceof StaggeredGridView) {
                ((StaggeredGridView) mList).addHeaderView(mListHeader);
            } else {
                throw new RuntimeException("List must extend ListView or StaggeredGridView");
            }
            // for parallax
            mList.setOnScrollListener(mScrollListener);
            setupDummyHeader();
        } else {
            //landscape mode header is added to the placeholder view
            ButterKnife.<ViewGroup>findById(this, R.id.hero_holder).addView(mListHeader);
        }
        mTitle.setText(presenter.getTitle(getContext()));
        mSubtitle.setText(presenter.getSubtitle(getContext()));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mIsStuck = ss.wasStuck;
        setupDummyHeader();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superstate = super.onSaveInstanceState();
        SavedState ss = new SavedState(superstate);
        ss.wasStuck = mIsStuck;
        return ss;
    }

    void setupDummyHeader() {
        if (mHeaderDummy != null) {
            //setup the dummy header background with the same color as the stickyheader
            final ClipDrawable dummyBackground = new ClipDrawable(mStickyHeader.getBackground(), Gravity.BOTTOM, ClipDrawable.VERTICAL);
            dummyBackground.setLevel(mIsStuck ? 10000 : 0);
            mHeaderDummy.setBackgroundDrawable(dummyBackground);
        }
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
                //int color = ThemeHelper.setColorAlpha(swatch.getRgb(), 0x99);//60%
                int color = swatch.getRgb();
                if (mHeaderDummy != null) {
                    final ClipDrawable dummyBackground =
                            new ClipDrawable(new ColorDrawable(color), Gravity.BOTTOM, ClipDrawable.VERTICAL);
                    dummyBackground.setLevel(mIsStuck ? 10000 : 0);
                    mHeaderDummy.setBackgroundDrawable(dummyBackground);
                }
                if (paletteResponse.shouldAnimate) {
                    final Drawable d = mStickyHeader.getBackground();
                    final Drawable d2 = new ColorDrawable(color);
                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{d,d2});
                    td.setCrossFadeEnabled(true);
                    mStickyHeader.setBackgroundDrawable(td);
                    td.startTransition(SquareImageView.TRANSITION_DURATION);
                } else {
                    mStickyHeader.setBackgroundColor(color);
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

    public static class SavedState extends BaseSavedState {
        boolean wasStuck;

        public SavedState(Parcel source) {
            super(source);
            wasStuck = source.readInt() == 1;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(wasStuck ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
