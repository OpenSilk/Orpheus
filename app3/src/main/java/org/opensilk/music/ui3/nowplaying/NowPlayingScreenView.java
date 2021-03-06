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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
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

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewLongClickEvent;
import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.CircleBarRenderer;
import com.pheelicks.visualizer.renderer.CircleRenderer;
import com.pheelicks.visualizer.renderer.Renderer;

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
import org.opensilk.music.ui3.common.UtilsCommon;

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
//    @InjectView(R.id.now_playing_card) CardView card;
    @InjectView(R.id.now_playing_progress) ProgressBar progress;
    @InjectView(R.id.now_playing_previous) ImageButton previousBtn;
    @InjectView(R.id.now_playing_next) ImageButton nextButton;
    @InjectView(R.id.now_playing_image) ImageView artwork;
    @InjectView(R.id.toolbar) Toolbar toolbar;

    final boolean lightTheme;

    VisualizerView visualizerView;
    String visualizerType = "none";
    int rendererColor = Color.argb(255, 222, 92, 143);

    CompositeSubscription clicks;
    Renderer currentRenderer;

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
                setupDrawables21();
            }
        }
        presenter.takeView(this);
    }

    @TargetApi(21)
    void setupDrawables21() {
        AnimatedStateListDrawable drawable = (AnimatedStateListDrawable) playPause.getDrawable();
        drawable.addTransition(R.id.pause_state, R.id.play_state, (AnimatedVectorDrawable)
                ContextCompat.getDrawable(getContext(), R.drawable.vector_pause_play_white_48dp), false);
        drawable.addTransition(R.id.play_state, R.id.pause_state, (AnimatedVectorDrawable)
                ContextCompat.getDrawable(getContext(), R.drawable.vector_play_pause_white_48dp), false);
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
                    visualizerView.setVisibility(GONE);
                    placeholder.addView(visualizerView);
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
        Renderer oldRenderer = currentRenderer;
        switch (visualizerType) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE: {
                Paint paint = new Paint();
                paint.setStrokeWidth(3f);
                paint.setAntiAlias(true);
                paint.setColor(rendererColor);
                CircleRenderer circleRenderer = new CircleRenderer(paint, false);
                visualizerView.addRenderer(circleRenderer);
                currentRenderer = circleRenderer;
                break;
            } case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR: {
                Paint paint = new Paint();
                paint.setStrokeWidth(8f);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                paint.setColor(rendererColor);
                CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 32, false);
                visualizerView.addRenderer(circleBarRenderer);
                currentRenderer = circleBarRenderer;
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
        if (oldRenderer != null) {
            visualizerView.removeRenderer(oldRenderer);
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
        linkVisualizer();
    }

    @DebugLog
    private void linkVisualizer() {
        destroyVisualizer();
        if (presenter.sessionId > 0) {
            if (visualizerView != null) {
                visualizerView.link(presenter.sessionId);
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
        Timber.d("artwork vis = %s", artwork.getVisibility() == VISIBLE ? "VISIBLE" : "INVISIBLE");
        if (visualizerView != null) {
            Timber.d("visulazer vis = %s", visualizerView.getVisibility() == VISIBLE ? "VISIBLE" : "GONE");
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
        view.clearAnimation();
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
        view.clearAnimation();
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
        @DebugLog
        public void onGenerated(Palette palette) {
            int cardBackground, titleText, subTitleText;
            int accent, buttons;

            if ((palette.getDarkVibrantSwatch() != null && palette.getVibrantSwatch() != null)
                    || (palette.getDarkMutedSwatch() != null && palette.getMutedSwatch() != null)) {
                PaletteSwatchType primary;
                Palette.Swatch s1 = palette.getDarkVibrantSwatch();
                Palette.Swatch s2 = palette.getVibrantSwatch();
                primary = PaletteSwatchType.VIBRANT_DARK;
                if (s1 == null || s2 == null) {
                    s1 = palette.getDarkMutedSwatch();
                    s2 = palette.getMutedSwatch();
                    primary = PaletteSwatchType.MUTED_DARK;
                }
                Timber.d("Themeing view");
                ViewBackgroundDrawableTarget.builder()
                        .into(getView())
                        .using(primary)
                        .build().onResourceReady(palette, backgroundTransition);

                cardBackground = s1.getRgb();
                titleText = s1.getTitleTextColor();
                subTitleText = s1.getBodyTextColor();
                accent = s2.getRgb();
                buttons = s1.getTitleTextColor();
            } else {
                Timber.d("Resetting view theme");
                int background = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.colorBackground);
                getView().setBackgroundColor(background);

                cardBackground = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        R.attr.nowPlayingCardBackground);
                titleText = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.textColorPrimary);
                subTitleText = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        android.R.attr.textColorSecondary);
                accent = ThemeUtils.getThemeAttrColor(getView().getContext(),
                        R.attr.colorAccent);
                buttons = ContextCompat.getColor(getContext(), R.color.white);
            }
//            getView().card.setCardBackgroundColor(cardBackground);
            getView().title.setTextColor(titleText);
            getView().subTitle.setTextColor(subTitleText);
            ThemeUtils.themeProgressBar2(getView().progress, accent);
            if (VersionUtils.hasLollipop()) {
                themeButtons(buttons);
            }
            getView().reInitRenderer(accent);
        }
    };

    static final int[] EMPTY_STATE_SET = new int[0];
    @TargetApi(21)
    void themeButtons(int color) {
        final int[][] states = new int[1][];
        final int[] colors = new int[1];
        states[0] = EMPTY_STATE_SET;
        colors[0] = color;
        final ColorStateList stateList = new ColorStateList(states, colors);
        previousBtn.setImageTintList(stateList);
        playPause.setImageTintList(stateList);
        nextButton.setImageTintList(stateList);
    }

}
