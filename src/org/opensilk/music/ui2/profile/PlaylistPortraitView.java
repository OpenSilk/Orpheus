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
import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 11/18/14.
 */
public class PlaylistPortraitView extends FrameLayout implements ProfileView {

    @Inject PlaylistScreen.Presenter presenter;

    @InjectView(android.R.id.list) PlaylistDragSortView mList;
    View mListHeader;
    FrameLayout mHeroContainer;
    AnimatedImageView mArtwork;
    AnimatedImageView mArtwork2;
    AnimatedImageView mArtwork3;
    AnimatedImageView mArtwork4;

    boolean mLightTheme;

    public PlaylistPortraitView(Context context, AttributeSet attrs) {
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

        mList.addHeaderView(mListHeader);

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
        return null;//unused
    }

}
