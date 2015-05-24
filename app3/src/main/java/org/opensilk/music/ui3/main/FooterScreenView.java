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
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteResponse;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;

/**
 * Created by drew on 10/15/14.
 */
public class FooterScreenView extends RelativeLayout {

    @Inject FooterScreenPresenter presenter;

    @InjectView(R.id.footer_thumbnail) AnimatedImageView artworkThumbnail;
    @InjectView(R.id.footer_progress) ProgressBar progressBar;
    @InjectView(R.id.footer_track_title) TextView trackTitle;
    @InjectView(R.id.footer_artist_name) TextView artistName;

    final boolean lightTheme;
    Subscription clicksSubscription;

    public FooterScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            FooterScreenComponent component = DaggerService.getDaggerComponent(getContext());
            component.inject(this);
        }
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
        clicksSubscription = ViewObservable.clicks(this).subscribe(
                new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.handleClick(getContext());
                    }
                }
        );
    }

    void unsubscribeClicks() {
        if (RxUtils.isSubscribed(clicksSubscription)) {
            clicksSubscription.unsubscribe();
            clicksSubscription = null;
        }
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
