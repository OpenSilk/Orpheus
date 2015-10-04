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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.media.audiofx.AudioEffect;
import android.support.v4.view.GravityCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;
import com.jakewharton.rxbinding.view.ViewLongClickEvent;
import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.CircleBarRenderer;
import com.pheelicks.visualizer.renderer.CircleRenderer;
import com.pheelicks.visualizer.renderer.LineRenderer;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.ImageButtonCheckable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.PaletteResponse;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_FILL;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_SCALE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_ARTWORK;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE_BAR;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_LINES;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingScreenView extends RelativeLayout {

    @Inject NowPlayingScreenPresenter presenter;
    @Inject AppPreferences settings;

    @InjectView(R.id.now_playing_something) ViewGroup placeholder;
    @InjectView(R.id.now_playing_title) TextView title;
    @InjectView(R.id.now_playing_subtitle) TextView subTitle;
    @InjectView(R.id.now_playing_playpause) ImageButtonCheckable playPause;
    @InjectView(R.id.now_playing_card) CardView card;
    @InjectView(R.id.now_playing_progress) ProgressBar progress;

    final boolean lightTheme;

    AnimatedImageView artwork;
    VisualizerView visualizerView;

    CompositeSubscription clicks;

    public NowPlayingScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lightTheme = ThemeUtils.isLightTheme(getContext());
        if (!isInEditMode()) {
            NowPlayingScreenComponent component = DaggerService.getDaggerComponent(getContext());
            component.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        placeholder.removeAllViews();
        String pickedview = settings.getString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_ARTWORK);
        switch (pickedview) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE:
            case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR:
            case NOW_PLAYING_VIEW_VIS_LINES:
                visualizerView = ViewUtils.inflate(getContext(), R.layout.now_playing_visualization, placeholder, false);
                placeholder.addView(visualizerView);
                initVisualizer(pickedview);
                break;
            case NOW_PLAYING_VIEW_ARTWORK:
            default:
                artwork = ViewUtils.inflate(getContext(), R.layout.now_playing_artwork, placeholder, false);
                placeholder.addView(artwork);
                initArtwork();
                break;
        }
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
        destroyVisualizer();
    }

    public void setPlayChecked(boolean yes) {
        playPause.setChecked(yes);
    }

    void initVisualizer(String type) {
        int accentColor = ThemeUtils.getColorAccent(getContext());
        int paintColor = Color.argb(255, 222, 92, 143);
        switch (type) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE: {
                Paint paint = new Paint();
                paint.setStrokeWidth(3f);
                paint.setAntiAlias(true);
                paint.setColor(paintColor);
                CircleRenderer circleRenderer = new CircleRenderer(paint, true);
                visualizerView.addRenderer(circleRenderer);
                break;
            } case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR: {
                Paint paint = new Paint();
                paint.setStrokeWidth(8f);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                paint.setColor(paintColor);
                CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 32, true);
                visualizerView.addRenderer(circleBarRenderer);
                break;
            } case NOW_PLAYING_VIEW_VIS_LINES: {
                Paint linePaint = new Paint();
                linePaint.setStrokeWidth(1f);
                linePaint.setAntiAlias(true);
                linePaint.setColor(paintColor);

                Paint lineFlashPaint = new Paint();
                lineFlashPaint.setStrokeWidth(5f);
                lineFlashPaint.setAntiAlias(true);
                lineFlashPaint.setColor(accentColor);
                LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
                visualizerView.addRenderer(lineRenderer);
                break;
            }
        }
//        attachVisualizer(presenter.sessionId);
    }

    @DebugLog
    public void attachVisualizer(int id) {
        destroyVisualizer();
        if (id == AudioEffect.ERROR_BAD_VALUE) return;
        if (visualizerView == null) return;
        visualizerView.link(id);
//        setVisualizerEnabled(presenter.isPlaying);
    }

    @DebugLog
    public void destroyVisualizer() {
        if (visualizerView == null) return;
        visualizerView.release();
    }

    @DebugLog
    public void setVisualizerEnabled(boolean enabled) {
        if (visualizerView == null) return;
        if (visualizerView.isLinked()) {
            visualizerView.setEnabled(enabled);
        }
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
    }

    public void setCurrentTime(CharSequence text) {
    }

    public void setCurrentTimeVisibility(int visibility) {

    }

    public int getCurrentTimeVisibility() {
        return VISIBLE;
    }

    public PaletteObserver getPaletteObserver() {
        return paletteObserver;
    }

    public void setCurrentTrack(CharSequence text) {
        title.setText(text);
    }

    public void setCurrentArtist(CharSequence text) {
        subTitle.setText(text);
    }

    void subscribeClicks() {
        if (isSubscribed(clicks)) return;
        clicks = new CompositeSubscription(
                RxView.clickEvents(playPause).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        presenter.playbackController.playorPause();
                    }
                }),
                RxView.longClickEvents(playPause).subscribe(new Action1<ViewLongClickEvent>() {
                    @Override
                    public void call(ViewLongClickEvent viewLongClickEvent) {
                        presenter.drawerController.openDrawer(GravityCompat.END);
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
                Palette p = paletteResponse.palette;
                Palette.Swatch s1 = p.getDarkVibrantSwatch();
                Palette.Swatch s2 = p.getVibrantSwatch();
                if (s1 != null && s2 != null) {
                    NowPlayingScreenView.this.setBackgroundColor(s1.getRgb());
                    progress.getProgressDrawable().setTint(s1.getRgb());
                    card.setBackgroundColor(s2.getRgb());
                    title.setTextColor(s2.getTitleTextColor());
                    subTitle.setTextColor(s2.getBodyTextColor());
                }
//                int color = p.getDarkVibrantColor(getResources().getColor(R.color.transparent_black));
//                color = ThemeUtils.setColorAlpha(color, 0x66);
//                PorterDuffColorFilter cf = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
//                artwork.setColorFilter(cf);
            }
        }
    };

}
