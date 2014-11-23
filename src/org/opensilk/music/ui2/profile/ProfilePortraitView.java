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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.opensilk.common.content.HeaderRecyclerAdapter;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 11/18/14.
 */
public class ProfilePortraitView extends FrameLayout implements ProfileView {

    @Inject BasePresenter presenter;

    @InjectView(android.R.id.list) RecyclerView mList;
    @InjectView(R.id.sticky_header) ViewGroup mStickyHeader;
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

    ProfileAdapter mAdapter;

    public ProfilePortraitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
        mLightTheme = ThemeUtils.isLightTheme(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        int headerlayout =  (presenter.getNumArtwork() >= 2) ?  R.layout.profile_hero4 : R.layout.profile_hero;
        mListHeader = LayoutInflater.from(getContext()).inflate(headerlayout, null);
        mHeroContainer = ButterKnife.findById(mListHeader, R.id.hero_container);
        mArtwork4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
        mArtwork3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
        mArtwork2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
        mArtwork = ButterKnife.findById(mHeroContainer, R.id.hero_image);

        mAdapter = presenter.makeAdapter(getContext());
        HeaderRecyclerAdapter<ProfileAdapter.ViewHolder> headerAdapter =
                new HeaderRecyclerAdapter<>(mAdapter);
        headerAdapter.addHeader(mListHeader);
        mList.setAdapter(headerAdapter);

        mList.setLayoutManager(getLayoutManager(getContext()));

        // for parallax
        mList.setOnScrollListener(mScrollListener);

        // sticky header
        setupStickyHeader();
        mTitle.setText(presenter.getTitle(getContext()));
        mSubtitle.setText(presenter.getSubtitle(getContext()));

        prepareRefresh();
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
        setupStickyHeader();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superstate = super.onSaveInstanceState();
        SavedState ss = new SavedState(superstate);
        ss.wasStuck = mIsStuck;
        return ss;
    }

    @Override
    public MortarScope getScope() {
        return Mortar.getScope(getContext());
    }

    @Override
    public AnimatedImageView getHero() {
        return mArtwork;
    }

    @Override
    public AnimatedImageView getHero2() {
        return mArtwork2;
    }

    @Override
    public AnimatedImageView getHero3() {
        return mArtwork3;
    }

    @Override
    public AnimatedImageView getHero4() {
        return mArtwork4;
    }

    @Override
    public ProfileAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public boolean isLandscape() {
        return false;
    }

    @Override
    public void prepareRefresh() {
        mList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver o = mList.getViewTreeObserver();
                if (o.isAlive()) {
                    o.removeOnPreDrawListener(this);
                }
                //when refreshing adapter need to reset parallax offset
                //does nothing on first load
                mHeroContainer.setTranslationY(0);
                //position stickyheader once recycler knows header bounds
                positionStickyHeader();
                return true;
            }
        });
    }

    RecyclerView.LayoutManager getLayoutManager(Context context) {
        if (presenter.isGrid()) {
            return makeGridLayoutManager(context);
        } else {
            return makeListLayoutManager(context);
        }
    }

    RecyclerView.LayoutManager makeGridLayoutManager(Context context) {
        final int numCols = context.getResources().getInteger(R.integer.profile_grid_cols_vertical);
        GridLayoutManager glm = new GridLayoutManager(context, numCols, GridLayoutManager.VERTICAL, false);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position == 0) ? numCols : 1;
            }
        });
        return glm;
    }

    RecyclerView.LayoutManager makeListLayoutManager(Context context) {
        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }

    void setupStickyHeader() {
        ClipDrawable d = new ClipDrawable(
                new ColorDrawable(ThemeUtils.getColorPrimary(getContext())),
                Gravity.BOTTOM, ClipDrawable.VERTICAL
        );
        d.setLevel(mIsStuck ? 10000 : 5000);
        mStickyHeader.setBackgroundDrawable(d);
    }

    void positionStickyHeader() {
        // sticky header
        final int top = mListHeader.getTop();
        final int stickyHeight = mStickyHeader.getMeasuredHeight();
        final int headerHeight = mListHeader.getMeasuredHeight();
        final int delta = headerHeight - stickyHeight;
        final int pos = delta + top;
        // reposition header
        mStickyHeader.setTranslationY(Math.max(pos,0));
        if (pos < 0 && !mIsStuck) {
            mIsStuck = true;
            makeSlideAnimator(5000, 10000, (ClipDrawable)mStickyHeader.getBackground()).start();
        } else if (pos > 0 && mIsStuck) {
            mIsStuck = false;
            makeSlideAnimator(10000, 5000, (ClipDrawable)mStickyHeader.getBackground()).start();
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
                final int color = swatch.getRgb();
                if (paletteResponse.shouldAnimate) {
                    mStickyHeader.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            final ClipDrawable d = new ClipDrawable(
                                    new ColorDrawable(color),
                                    Gravity.BOTTOM, ClipDrawable.VERTICAL
                            );
                            d.setLevel(mIsStuck ? 10000 : 5000);
                            mStickyHeader.setBackgroundDrawable(d);
                            mStickyHeader.animate().alpha(1f).start();
                        }
                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }
                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    }).start();
                } else {
                    mStickyHeader.setBackgroundColor(color);
                }
            }
        }
    };

    final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            // logic here derived from http://antoine-merle.com/blog/2013/10/04/making-that-google-plus-profile-screen/
            if (mList.getChildCount() == 0) return;
            if (mList.getChildViewHolder(mList.getChildAt(0)).itemView == mListHeader
                    && mList.getChildCount() > 1) {
                // parallax
                mHeroContainer.setTranslationY(-mList.getChildAt(0).getTop() / 2);
            }
            positionStickyHeader();
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
