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

package org.opensilk.music.ui.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.SquareImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.common.glide.Paletteable;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/31/14.
 */
public class GridTileDescription extends LinearLayout implements Paletteable {

    @InjectView(R.id.tile_title) TextView mTitle;
    @InjectView(R.id.tile_subtitle) TextView mSubTitle;

    final PaletteObserver paletteObserver;
    final boolean lightTheme;
    Drawable originalBackground;

    public GridTileDescription(Context context, AttributeSet attrs) {
        super(context, attrs);
        lightTheme = ThemeUtils.isLightTheme(getContext());
        paletteObserver = new PaletteObserver() {
            @Override
            public void onNext(PaletteResponse paletteResponse) {
                Palette palette = paletteResponse.palette;
                Palette.Swatch swatch = lightTheme ? palette.getLightVibrantSwatch() : palette.getDarkVibrantSwatch();
                if (swatch == null) swatch = palette.getVibrantSwatch();
                if (swatch != null) {
                    if (paletteResponse.shouldAnimate) {
                        originalBackground = getBackground();
                        Drawable d = getBackground();
                        Drawable d2 = new ColorDrawable(swatch.getRgb());
                        TransitionDrawable td = new TransitionDrawable(new Drawable[]{d,d2});
                        td.setCrossFadeEnabled(true);
                        setBackgroundDrawable(td);
                        td.startTransition(SquareImageView.TRANSITION_DURATION);
                    } else {
                        originalBackground = getBackground();
                        setBackgroundColor(swatch.getRgb());
//                        mTitle.setTextColor(swatch.getTitleTextColor());
//                        mSubTitle.setTextColor(swatch.getBodyTextColor());
                    }
                }
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public PaletteObserver getPaletteObserver() {
        return paletteObserver;
    }

    public void resetBackground() {
        if (originalBackground != null) {
            setBackgroundDrawable(originalBackground);
        }
    }

    public TextView getTitle() {
        return mTitle;
    }

    public TextView getSubTitle() {
        return mSubTitle;
    }

    @Override
    public List<? extends View> getBackgroundViews() {
        return Collections.singletonList(this);
    }

    @Override
    public List<TextView> getTitleViews() {
        return Collections.singletonList(mTitle);
    }

    @Override
    public List<TextView> getBodyViews() {
        return Collections.singletonList(mSubTitle);
    }
}
