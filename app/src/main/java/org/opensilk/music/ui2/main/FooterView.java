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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteResponse;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;

/**
 * Created by drew on 10/15/14.
 */
public class FooterView extends RelativeLayout {

    @Inject FooterPresenter presenter;

    @InjectView(R.id.footer_thumbnail) AnimatedImageView artworkThumbnail;
    @InjectView(R.id.footer_progress) ProgressBar progressBar;
    @InjectView(R.id.footer_track_title) TextView trackTitle;
    @InjectView(R.id.footer_artist_name) TextView artistName;

    final boolean lightTheme;

    public FooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(context, this);
        lightTheme = ThemeUtils.isLightTheme(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            ThemeUtils.themeProgressBar(progressBar, R.attr.colorAccent);
            presenter.takeView(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscribeClicks();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        if (!isInEditMode()) presenter.dropView(this);
    }

    void subscribeClicks() {
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onClick(getContext());
            }
        });
        this.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return presenter.onLongClick(getContext());
            }
        });
        artworkThumbnail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onThumbClick(getContext());
            }
        });
        artworkThumbnail.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return presenter.onThumbLongClick(getContext());
            }
        });
    }

    void unsubscribeClicks() {
        this.setOnClickListener(null);
        this.setOnLongClickListener(null);
        artworkThumbnail.setOnClickListener(null);
        artworkThumbnail.setOnLongClickListener(null);
    }

    public void updateBackground(PaletteResponse paletteResponse) {
        Palette palette = paletteResponse.palette;
        Palette.Swatch swatch = lightTheme
                ? palette.getLightMutedSwatch() : palette.getDarkMutedSwatch();
        if (swatch == null) swatch = palette.getMutedSwatch();
        Drawable d1;
        if (getBackground() == null) {
            d1 = new ColorDrawable(Color.TRANSPARENT);
        } else {
            d1 = getBackground();
            if (d1 instanceof TransitionDrawable) {
                d1 = ((TransitionDrawable)d1).getDrawable(1);
            }
        }
        Drawable d2;
        if (swatch != null) {
            d2 = new ColorDrawable(swatch.getRgb());
        } else {
            d2 = new ColorDrawable(Color.TRANSPARENT);
        }
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{d1,d2});
        td.setCrossFadeEnabled(true);
        setBackgroundDrawable(td);
        td.startTransition(600);
    }
}
