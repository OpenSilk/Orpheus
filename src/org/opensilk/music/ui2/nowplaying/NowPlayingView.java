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

package org.opensilk.music.ui2.nowplaying;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.triggertrap.seekarc.SeekArc;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.util.VersionUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.ImageButtonCheckable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.theme.PlaybackDrawableTint;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingView extends RelativeLayout {

    @Inject NowPlayingScreen.Presenter presenter;

    @InjectView(R.id.now_playing_toolbar) Toolbar toolbar;
    @InjectView(R.id.now_playing_image) AnimatedImageView artwork;
    @InjectView(R.id.now_playing_seekprogress) SeekBar seekBar;
    @InjectView(R.id.now_playing_current_time) TextView currentTime;
    @InjectView(R.id.now_playing_total_time) TextView totalTime;
    @InjectView(R.id.now_playing_shuffle) ImageButton shuffle;
    @InjectView(R.id.now_playing_previous) ImageButton prev;
    @InjectView(R.id.now_playing_play) ImageButtonCheckable play;
    @InjectView(R.id.now_playing_next) ImageButton next;
    @InjectView(R.id.now_playing_repeat) ImageButton repeat;

    CompositeSubscription clicks;
    Drawable origRepeat;
    Drawable origShuffle;

    public NowPlayingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        if (!(seekBar instanceof SeekArc)) {
            ThemeUtils.themeSeekBar(seekBar, R.attr.colorAccent);
        } else if (!VersionUtils.hasLollipop()) {
            ((SeekArc)seekBar).getThumb().mutate().setColorFilter(
                    ThemeUtils.getColorAccent(getContext()), PorterDuff.Mode.SRC_IN
            );
        }
        PlaybackDrawableTint.repeatDrawable36(repeat);
        origRepeat = repeat.getDrawable();
        PlaybackDrawableTint.shuffleDrawable36(shuffle);
        origShuffle = shuffle.getDrawable();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        seekBar.setOnSeekBarChangeListener(presenter);
        if (!isInEditMode()) presenter.takeView(this);
        subscribeClicks();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        presenter.dropView(this);
        seekBar.setOnSeekBarChangeListener(null);
    }

    void subscribeClicks() {
        if (isSubscribed(clicks)) return;
        clicks = new CompositeSubscription(
                ViewObservable.clicks(shuffle).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.musicService.cycleShuffleMode();
                    }
                }),
                ViewObservable.clicks(prev).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.musicService.prev();
                    }
                }),
                ViewObservable.clicks(play).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.musicService.playOrPause();
                    }
                }),
                ViewObservable.clicks(next).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.musicService.next();
                    }
                }),
                ViewObservable.clicks(repeat).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        presenter.musicService.cycleRepeatMode();
                    }
                })
        );
    }

    void unsubscribeClicks() {
        if (isSubscribed(clicks)) {
            clicks.unsubscribe();
            clicks = null;
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    void colorize(Palette p) {
        if (true) return; //XXX STUBBED
        if (!presenter.settings.getBoolean(AppPreferences.NOW_PLAYING_COLORIZE, true)) return;
        final String option = presenter.settings.getString(AppPreferences.NOW_PLAYING_PALETTE,
                AppPreferences.NOW_PLAYING_PALETTE_VIBRANT_AB_MUTED_BDY);
        Palette.Swatch s;
        Palette.Swatch s2;
        int btnColor;
        switch (option) {
            case AppPreferences.NOW_PLAYING_PALETTE_MUTED:
                s = p.getDarkMutedSwatch();
                s2 = p.getMutedSwatch();
                btnColor = p.getDarkVibrantColor(s2 != null ? getComplimentary(s2.getHsl()) : 0);
                break;
            case AppPreferences.NOW_PLAYING_PALETTE_MUTED_FALLBACK:
                s = p.getDarkMutedSwatch();
                s2 = p.getMutedSwatch();
                btnColor = p.getDarkVibrantColor(s2 != null ? getComplimentary(s2.getHsl()) : 0);
                if (s == null || s2 == null) {
                    s = p.getDarkVibrantSwatch();
                    s2 = p.getVibrantSwatch();
                    btnColor = s2 != null ? getComplimentary(s2.getHsl()) : 0;
                }
                break;
            case AppPreferences.NOW_PLAYING_PALETTE_VIBRANT:
                s = p.getDarkVibrantSwatch();
                s2 = p.getVibrantSwatch();
                btnColor = s2 != null ? getComplimentary(s2.getHsl()) : 0;
                break;
            case AppPreferences.NOW_PLAYING_PALETTE_VIBRANT_FALLBACK:
                s = p.getDarkVibrantSwatch();
                s2 = p.getVibrantSwatch();
                btnColor = s2 != null ? getComplimentary(s2.getHsl()) : 0;
                if (s == null || s2 == null) {
                    s = p.getDarkMutedSwatch();
                    s2 = p.getMutedSwatch();
                    btnColor = p.getDarkVibrantColor(s2 != null ? getComplimentary(s2.getHsl()) : 0);
                }
                break;
            case AppPreferences.NOW_PLAYING_PALETTE_VIBRANT_AB_MUTED_BDY:
            default:
                s = p.getVibrantSwatch();
                s2 = p.getMutedSwatch();
                btnColor = p.getDarkVibrantColor(s2 != null ? getComplimentary(s2.getHsl()) : 0);
        }
        if (s != null && s2 != null) {
            toolbar.setBackgroundColor(s.getRgb());
            setBackgroundColor(s2.getRgb());
            if (!(seekBar instanceof SeekArc)) {
                ThemeUtils.themeSeekBar2(seekBar, btnColor);
            } else if (!VersionUtils.hasLollipop()) {
                ((SeekArc)seekBar).getThumb().mutate().setColorFilter(
                        ThemeUtils.getColorAccent(getContext()), PorterDuff.Mode.SRC_IN
                );
            }
            repeat.setImageDrawable(PlaybackDrawableTint.getRepeatDrawable36(getContext(), btnColor));
            shuffle.setImageDrawable(PlaybackDrawableTint.getShuffleDrawable36(getContext(), btnColor));
        } else {
            toolbar.setBackgroundColor(ThemeUtils.getColorPrimary(getContext()));
            setBackgroundColor(ThemeUtils.getThemeAttrColor(getContext(), android.R.attr.colorBackground));
            if (!(seekBar instanceof SeekArc)) {
                ThemeUtils.themeSeekBar(seekBar, R.attr.colorAccent);
            } else if (!VersionUtils.hasLollipop()) {
                ((SeekArc)seekBar).getThumb().mutate().setColorFilter(
                        ThemeUtils.getColorAccent(getContext()), PorterDuff.Mode.SRC_IN
                );
            }
            repeat.setImageDrawable(origRepeat);
            shuffle.setImageDrawable(origShuffle);
        }
    }

    /*
     * HSLColor
     */
    static int getComplimentary(float[] hsl)
    {
        float hue = (hsl[0] + 180.0f) % 360.0f;
        return HSLtoRGB(hue, hsl[1], hsl[2]);
    }

    /*
     * AOSP Palette#ColorUtils
     */
    static int HSLtoRGB (final float h, final float s, final float l) {
        final float c = (1f - Math.abs(2 * l - 1f)) * s;
        final float m = l - 0.5f * c;
        final float x = c * (1f - Math.abs((h / 60f % 2f) - 1f));

        final int hueSegment = (int) h / 60;

        int r = 0, g = 0, b = 0;

        switch (hueSegment) {
            case 0:
                r = Math.round(255 * (c + m));
                g = Math.round(255 * (x + m));
                b = Math.round(255 * m);
                break;
            case 1:
                r = Math.round(255 * (x + m));
                g = Math.round(255 * (c + m));
                b = Math.round(255 * m);
                break;
            case 2:
                r = Math.round(255 * m);
                g = Math.round(255 * (c + m));
                b = Math.round(255 * (x + m));
                break;
            case 3:
                r = Math.round(255 * m);
                g = Math.round(255 * (x + m));
                b = Math.round(255 * (c + m));
                break;
            case 4:
                r = Math.round(255 * (x + m));
                g = Math.round(255 * m);
                b = Math.round(255 * (c + m));
                break;
            case 5:
            case 6:
                r = Math.round(255 * (c + m));
                g = Math.round(255 * m);
                b = Math.round(255 * (x + m));
                break;
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return Color.rgb(r, g, b);
    }
}
