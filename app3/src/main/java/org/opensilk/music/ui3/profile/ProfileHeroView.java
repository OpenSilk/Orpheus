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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by drew on 5/5/15.
 */
public class ProfileHeroView extends FrameLayout {

    @Inject ProfileHeroViewPresenter mPresenter;

    @InjectView(R.id.hero_image) AnimatedImageView mArtwork;
    @InjectView(R.id.hero_image2) @Optional AnimatedImageView mArtwork2;
    @InjectView(R.id.hero_image3) @Optional AnimatedImageView mArtwork3;
    @InjectView(R.id.hero_image4) @Optional AnimatedImageView mArtwork4;

    public ProfileHeroView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ProfileHeroComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }
}
