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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.CompatSeekBar;
import org.opensilk.common.ui.widget.ImageButtonCheckable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;
import org.opensilk.music.playback.control.PlaybackController;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_FILL;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_SCALE;

/**
 * Created by drew on 5/24/15.
 */
public class CarModeScreenView extends RelativeLayout implements NowPlayingView {

    @Inject NowPlayingViewPresenter presenter;
    @Inject AppPreferences settings;
    @Inject PlaybackController playbackController;

    @InjectView(R.id.now_playing_actions_container) ViewGroup actionsContainer;
    @InjectView(R.id.now_playing_seekbar) CompatSeekBar seekBarClassic;
    @InjectView(R.id.now_playing_current_time) TextView currentTime;
    @InjectView(R.id.now_playing_total_time) TextView totalTime;
    @InjectView(R.id.now_playing_previous) ImageButton prev;
    @InjectView(R.id.now_playing_play) ImageButtonCheckable play;
    @InjectView(R.id.now_playing_next) ImageButton next;
    @InjectView(R.id.now_playing_image) AnimatedImageView artwork;
    @InjectView(R.id.now_playing_track) TextView track;
    @InjectView(R.id.now_playing_artist) TextView artist;

    final boolean lightTheme;
    CompositeSubscription clicks;

    public CarModeScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lightTheme = ThemeUtils.isLightTheme(getContext());
        if (!isInEditMode()) {
            CarModeScreenComponent component = DaggerService.getDaggerComponent(getContext());
            component.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        setupSeekbar();
        subscribeClicks();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) presenter.takeView(this);
        subscribeClicks();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        presenter.dropView(this);
    }

    void setupSeekbar() {
        if (!VersionUtils.hasLollipop()) {
            ThemeUtils.themeSeekBar(seekBarClassic, R.attr.colorAccent);
        }
    }

    public void setProgress(int progress) {
        seekBarClassic.setProgress(progress);
    }

    void initArtwork() {
        String scaleType = settings.getString(NOW_PLAYING_ARTWORK_SCALE, NOW_PLAYING_ARTWORK_FILL);
        if (NOW_PLAYING_ARTWORK_FILL.equals(scaleType)) {
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    public AnimatedImageView getArtwork() {
        return artwork;
    }

    public void setTotalTime(CharSequence text) {
        totalTime.setText(text);
    }

    public void setCurrentTime(CharSequence text) {
        currentTime.setText(text);
    }

    public void setCurrentTimeVisibility(int visibility) {
        currentTime.setVisibility(visibility);
    }

    public int getCurrentTimeVisibility() {
        return currentTime.getVisibility();
    }

    public PaletteObserver getPaletteObserver() {
        return paletteObserver;
    }

    public void setPlayChecked(boolean yes) {
        play.setChecked(yes);
    }

    public void setCurrentTrack(CharSequence text) {
        track.setText(text);
    }

    public void setCurrentArtist(CharSequence text) {
        artist.setText(text);
    }

    @Override
    public void attachVisualizer(int id) {

    }

    @Override
    public void destroyVisualizer() {

    }

    @Override
    public void setVisualizerEnabled(boolean enabled) {

    }

    void subscribeClicks() {
        if (isSubscribed(clicks)) return;
        clicks = new CompositeSubscription(
                RxView.clickEvents(prev).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        playbackController.skipToPrevious();
                    }
                }),
                RxView.clickEvents(play).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        playbackController.playorPause();
                    }
                }),
                RxView.clickEvents(next).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        playbackController.skipToNext();
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

    final PaletteObserver paletteObserver = new PaletteObserver() {
        @Override
        public void onNext(PaletteResponse paletteResponse) {
            if (artwork != null) {
//                Palette p = paletteResponse.palette;
//                Palette.Swatch s1 = p.getVibrantSwatch();
//                Palette.Swatch s2 = p.getDarkVibrantSwatch();
//                int artworkOverlay;
//                if (s1 != null && s2 != null) {
//                    track.setTextColor(s1.getTitleTextColor());
//                    artist.setTextColor(s1.getTitleTextColor());
//                    currentTime.setTextColor(s1.getBodyTextColor());
//                    totalTime.setTextColor(s1.getBodyTextColor());
//                    artworkOverlay = s1.getRgb();
//                    ThemeUtils.themeSeekBar2(seekBarClassic, s2.getRgb());
//                } else {
//                    int textPrimary = ThemeUtils.getThemeAttrColor(getContext(),
//                            android.R.attr.textColorPrimary);
//                    int textSecondary = ThemeUtils.getThemeAttrColor(getContext(),
//                            android.R.attr.textColorSecondary);
//                    track.setTextColor(textPrimary);
//                    artist.setTextColor(textSecondary);
//                    currentTime.setTextColor(textPrimary);
//                    totalTime.setTextColor(textPrimary);
//                    artworkOverlay = getResources().getColor(R.color.transparent_black);
//                    ThemeUtils.themeSeekBar(seekBarClassic, R.attr.colorAccent);
//                }
//                artworkOverlay = ThemeUtils.setColorAlpha(artworkOverlay, 0x73 /*0x66*/);
//                PorterDuffColorFilter cf = new PorterDuffColorFilter(artworkOverlay,
//                        PorterDuff.Mode.SRC_ATOP);
//                artwork.setColorFilter(cf);
                Palette p = paletteResponse.palette;
                int color = p.getDarkVibrantColor(getResources().getColor(R.color.transparent_black));
                color = ThemeUtils.setColorAlpha(color, 0x73 /*0x66*/);
                PorterDuffColorFilter cf = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                artwork.setColorFilter(cf);
            }
        }
    };
}
