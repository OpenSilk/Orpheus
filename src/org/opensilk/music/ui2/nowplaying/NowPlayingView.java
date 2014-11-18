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
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;

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
    @InjectView(R.id.now_playing_play) ImageButton play;
    @InjectView(R.id.now_playing_next) ImageButton next;
    @InjectView(R.id.now_playing_repeat) ImageButton repeat;

    CompositeSubscription clicks;

    public NowPlayingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        ThemeUtils.themeSeekBar(seekBar, R.attr.colorAccent);
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscribeClicks();
        seekBar.setOnSeekBarChangeListener(presenter);
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        seekBar.setOnSeekBarChangeListener(null);
        presenter.dropView(this);
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
        boolean light = ThemeUtils.isLightTheme(getContext());
        Palette.Swatch s = light ? p.getLightVibrantSwatch() : p.getDarkVibrantSwatch();
        if (s == null) s = p.getVibrantSwatch();
        Palette.Swatch s2 = light ? p.getLightMutedSwatch() : p.getDarkMutedSwatch();
        if (s2 == null) s2 = p.getMutedSwatch();
        if (s != null) {
            setBackgroundColor(s.getRgb());
//            toolbar.setTitleTextColor(s.getTitleTextColor());
//            toolbar.setSubtitleTextColor(s.getTitleTextColor());
        } else {
            setBackgroundColor(ThemeUtils.getThemeAttrColor(getContext(), R.attr.colorPrimary));
//            toolbar.setTitleTextColor(Color.WHITE);
//            toolbar.setSubtitleTextColor(Color.WHITE);
        }
        if (s2 != null) {
            ThemeUtils.themeSeekBar2(seekBar, s2.getRgb());
        } else {
            ThemeUtils.themeSeekBar(seekBar, R.attr.colorAccent);
        }
    }
}
