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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.transition.Transition;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewLongClickEvent;
import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.CircleBarRenderer;
import com.pheelicks.visualizer.renderer.CircleRenderer;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.glide.PalettableUtils;
import org.opensilk.common.glide.PaletteSwatchType;
import org.opensilk.common.glide.PaletteableDrawableCrossFadeTransition;
import org.opensilk.common.glide.ViewBackgroundDrawableTarget;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.common.ui.widget.ImageButtonCheckable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE_BAR;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_LINES;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingScreenView extends RelativeLayout {

    @Inject NowPlayingScreenPresenter presenter;
    @Inject AppPreferences settings;
    @Inject ToolbarOwner toolbarOwner;

    @InjectView(R.id.now_playing_something) ViewGroup placeholder;
    @InjectView(R.id.now_playing_title) TextView title;
    @InjectView(R.id.now_playing_subtitle) TextView subTitle;
    @InjectView(R.id.now_playing_playpause) ImageButtonCheckable playPause;
    @InjectView(R.id.now_playing_card) CardView card;
    @InjectView(R.id.now_playing_progress) ProgressBar progress;
    @InjectView(R.id.now_playing_previous) ImageButton previousBtn;
    @InjectView(R.id.now_playing_next) ImageButton nextButton;
    @InjectView(R.id.now_playing_image) ImageView artwork;
    @InjectView(R.id.toolbar) Toolbar toolbar;

    final boolean lightTheme;

    VisualizerView visualizerView;
    String visualizerType = "none";
    int rendererColor = Color.argb(255, 222, 92, 143);
    int sessionId = 0;

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
    @DebugLog
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        if (!isInEditMode()) {
            visualizerType = settings.getString(NOW_PLAYING_VIEW, "none");
            rendererColor = ThemeUtils.getColorAccent(getContext());
            if (VersionUtils.hasLollipop()) {
                AnimatedStateListDrawable drawable = (AnimatedStateListDrawable) playPause.getDrawable();
                drawable.addTransition(R.id.pause_state, R.id.play_state, (AnimatedVectorDrawable)
                        ContextCompat.getDrawable(getContext(), R.drawable.ic_pause_play_white_animated_48dp), false);
                drawable.addTransition(R.id.play_state, R.id.pause_state, (AnimatedVectorDrawable)
                        ContextCompat.getDrawable(getContext(), R.drawable.ic_play_pause_white_animated_48dp), false);
            }
        }
        presenter.takeView(this);
    }

    @Override
    @DebugLog
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            toolbarOwner.attachToolbar(toolbar);
            toolbarOwner.setConfig(presenter.getActionBarConfig());
            presenter.takeView(this);
            subscribeClicks();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        toolbarOwner.detachToolbar(toolbar);
        presenter.dropView(this);
        destroyVisualizer();
    }

    public void setPlayChecked(boolean yes) {
        playPause.setChecked(yes);
    }

    @DebugLog
    private void initVisualizer() {
        View vis = placeholder.findViewById(R.id.now_playing_visualizer);
        if (vis != null) placeholder.removeView(vis);
        visualizerView = null;
        switch (visualizerType) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE:
            case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR:
            case NOW_PLAYING_VIEW_VIS_LINES: {
                if (PackageManager.PERMISSION_GRANTED
                        == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)) {
                    visualizerView = ViewUtils.inflate(getContext(),
                            R.layout.now_playing_visualization, placeholder, false);
                    placeholder.addView(visualizerView);
                    placeholder.bringChildToFront(visualizerView);
                    initRenderer();
                } else {
                    //TODO
                }
                break;
            }
        }
    }

    @DebugLog
    private void initRenderer() {
        visualizerView.clearRenderers();
        switch (visualizerType) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE: {
                Paint paint = new Paint();
                paint.setStrokeWidth(3f);
                paint.setAntiAlias(true);
                paint.setColor(rendererColor);
                CircleRenderer circleRenderer = new CircleRenderer(paint, false);
                visualizerView.addRenderer(circleRenderer);
                break;
            } case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR: {
                Paint paint = new Paint();
                paint.setStrokeWidth(8f);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                paint.setColor(rendererColor);
                CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 32, false);
                visualizerView.addRenderer(circleBarRenderer);
                break;
            } case NOW_PLAYING_VIEW_VIS_LINES: {
                /*
                int accentColor = ThemeUtils.getColorAccent(getContext());
                Paint linePaint = new Paint();
                linePaint.setStrokeWidth(1f);
                linePaint.setAntiAlias(true);
                linePaint.setColor(rendererColor);

                Paint lineFlashPaint = new Paint();
                lineFlashPaint.setStrokeWidth(5f);
                lineFlashPaint.setAntiAlias(true);
                lineFlashPaint.setColor(accentColor);
                LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
                visualizerView.addRenderer(lineRenderer);
                */
                break;
            }
        }
    }

    public void reInitRenderer() {
        if (visualizerView != null) {
            initRenderer();
        }
    }

    public void reInitRenderer(int paintColor) {
        rendererColor = paintColor;
        reInitRenderer();
    }

    public void relinkVisualizer(int sessionId) {
        this.sessionId = sessionId;
        linkVisualizer();
    }

    @DebugLog
    private void linkVisualizer() {
        destroyVisualizer();
        if (sessionId > 0) {
            if (visualizerView != null) {
                visualizerView.link(sessionId);
            }
        }
    }

    @DebugLog
    public void destroyVisualizer() {
        if (visualizerView != null) {
            visualizerView.release();
        }
    }

    public void disableVisualizer(){
        setPlaying(false);
        initVisualizer();
    }

    public void setPlaying(boolean playing) {
        if (playing) {
            if (visualizerView == null) {
                initVisualizer();
            }
            if (visualizerView != null) {
                if (!visualizerView.isLinked()) {
                    Timber.d("Relinking visualizer");
                    linkVisualizer();
                }
                if (visualizerView.isLinked()) {
                    Timber.d("Visualizer linked");
                    //success! show the visualizer
                    visualizerView.setEnabled(true);
                    if (visualizerView.getVisibility() != VISIBLE) {
                        animateIn(visualizerView);
                    }
                    if (artwork.getVisibility() == VISIBLE) {
                        animateOut(artwork);
                    }
                } else {
                    Timber.w("Failed linking visualizer");
                    //link failed go back to artwork
                    if (visualizerView.getVisibility() == VISIBLE) {
                        animateOut(visualizerView);
                    }
                    if (artwork.getVisibility() != VISIBLE) {
                        animateIn(artwork);
                    }
                }
            } else if (artwork.getVisibility() != VISIBLE) {
                //no visualizer always want art
                animateIn(artwork);
            }
        } else {
            if (visualizerView != null) {
                //eagerly release in case paused for a while
                destroyVisualizer();
                if (visualizerView.getVisibility() == VISIBLE) {
                    animateOut(visualizerView);
                }
            }
            //always want artwork when paused
            if (artwork.getVisibility() != VISIBLE) {
                animateIn(artwork);
            }
        }
    }

    public ImageView getArtwork() {
        return artwork;
    }

    public void setCurrentTrack(CharSequence text) {
        title.setText(text);
    }

    public void setCurrentArtist(CharSequence text) {
        subTitle.setText(text);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    void subscribeClicks() {
        if (isSubscribed(clicks)) return;
        clicks = new CompositeSubscription(
                RxView.clicks(playPause).subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        presenter.playbackController.playorPause();
                    }
                }),
                RxView.longClickEvents(playPause).subscribe(new Action1<ViewLongClickEvent>() {
                    @Override
                    public void call(ViewLongClickEvent viewLongClickEvent) {
                        presenter.drawerController.openDrawer(GravityCompat.END);
                    }
                }),
                RxView.clicks(previousBtn).subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        presenter.playbackController.skipToPrevious();
                    }
                }),
                RxView.clicks(nextButton).subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        presenter.playbackController.skipToNext();
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

    @DebugLog
    private void animateOut(View view) {
        view.setAlpha(1f);
        view.setVisibility(VISIBLE);
        ViewCompat.animate(view)
                .alpha(0f)
                .setDuration(300)
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    @DebugLog
                    public void onAnimationEnd(View view) {
                        if (view == artwork) {
                            view.setVisibility(INVISIBLE); //not gone or palette wont update
                        } else {
                            view.setVisibility(GONE);
                        }
                        view.setAlpha(1f);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                    }
                })
                .start();
    }

    @DebugLog
    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(VISIBLE);
        ViewCompat.animate(view)
                .alpha(1f)
                .setDuration(300)
                .setListener(null)
                .start();
    }

    private NowPlayingScreenView getView() {
        return this;
    }

    final PaletteableDrawableCrossFadeTransition backgroundTransition =
            new PaletteableDrawableCrossFadeTransition(PalettableUtils.DEFAULT_DURATION_MS);

    final Palette.PaletteAsyncListener mListener = new Palette.PaletteAsyncListener() {
        @Override
        public void onGenerated(Palette palette) {
            Palette.Swatch s1 = palette.getDarkVibrantSwatch();
            Palette.Swatch s2 = palette.getVibrantSwatch();
            if (s1 != null && s2 != null) {
                ViewBackgroundDrawableTarget.builder()
                        .into(getView())
                        .using(PaletteSwatchType.VIBRANT_DARK)
                        .build().onResourceReady(palette, backgroundTransition);
                ViewBackgroundDrawableTarget.builder()
                        .into(card)
                        .using(PaletteSwatchType.VIBRANT)
                        .build().onResourceReady(palette, backgroundTransition);
                getView().title.setTextColor(s2.getTitleTextColor());
                getView().subTitle.setTextColor(s2.getBodyTextColor());
                getView().progress.getProgressDrawable().setTint(s1.getRgb());
                getView().reInitRenderer(s1.getRgb());
            } else {
                int background = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.colorBackground);
                getView().setBackgroundColor(background);
                int cardBackground = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        R.attr.nowPlayingCardBackground);
                getView().card.setBackgroundColor(cardBackground);
                int titleText = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.textColorPrimary);
                getView().title.setTextColor(titleText);
                int subTitleText = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.textColorSecondary);
                getView().subTitle.setTextColor(subTitleText);
                int accent = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        R.attr.colorAccent);
                getView().progress.getProgressDrawable().setTint(accent);
                getView().reInitRenderer(accent);
            }
        }
    };

}
