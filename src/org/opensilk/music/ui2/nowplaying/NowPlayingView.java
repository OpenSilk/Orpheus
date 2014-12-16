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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import butterknife.Optional;
import mortar.Mortar;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_START_SCREEN;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_SCREEN_ARTWORK;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_SCREEN_CONTROLS;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_SCALE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_FILL;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_ARTWORK_FIT;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingView extends RelativeLayout {

    @Inject NowPlayingScreen.Presenter presenter;

    @InjectView(R.id.now_playing_image) AnimatedImageView artwork;
    @InjectView(R.id.now_playing_actions_container) ViewGroup actionsContainer;
    @InjectView(R.id.now_playing_seekprogress) SeekArc seekBar;
    @InjectView(R.id.now_playing_current_time) TextView currentTime;
    @InjectView(R.id.now_playing_total_time) TextView totalTime;
    @InjectView(R.id.now_playing_shuffle) ImageButton shuffle;
    @InjectView(R.id.now_playing_previous) ImageButton prev;
    @InjectView(R.id.now_playing_play) ImageButtonCheckable play;
    @InjectView(R.id.now_playing_next) ImageButton next;
    @InjectView(R.id.now_playing_repeat) ImageButton repeat;
    @InjectView(R.id.now_playing_btn_flip) @Optional ImageButton btnFlip;

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
        if (btnFlip != null) {
            String startScreen = presenter.settings.getString(NOW_PLAYING_START_SCREEN, NOW_PLAYING_SCREEN_ARTWORK);
            if (NOW_PLAYING_SCREEN_ARTWORK.equals(startScreen)) {
                artwork.setVisibility(VISIBLE);
                actionsContainer.setVisibility(GONE);
            } else {
                artwork.setVisibility(GONE);
                actionsContainer.setVisibility(VISIBLE);
            }
            String scaleType = presenter.settings.getString(NOW_PLAYING_ARTWORK_SCALE, NOW_PLAYING_ARTWORK_FILL);
            if (NOW_PLAYING_ARTWORK_FILL.equals(scaleType)) {
                artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        } else { //landscape
            artwork.setVisibility(VISIBLE);
            actionsContainer.setVisibility(VISIBLE);
        }
        if (!VersionUtils.hasLollipop()) {
            seekBar.getThumb().mutate().setColorFilter(
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
        seekBar.setOnSeekArcChangeListener(presenter);
        if (!isInEditMode()) presenter.takeView(this);
        subscribeClicks();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        presenter.dropView(this);
        seekBar.setOnSeekArcChangeListener(null);
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
        if (btnFlip != null) {
            clicks.add(ViewObservable.clicks(btnFlip).subscribe(new Action1<OnClickEvent>() {
                @Override
                public void call(OnClickEvent onClickEvent) {
                    flip();
                }
            }));
        }
    }

    void unsubscribeClicks() {
        if (isSubscribed(clicks)) {
            clicks.unsubscribe();
            clicks = null;
        }
    }

    private Interpolator accelerator = new AccelerateInterpolator();
    private Interpolator decelerator = new DecelerateInterpolator();
    private static final int TRANSITION_DURATION = 160;
    void flip() {
        final View visibleLayout;
        final View invisibleLayout;
        if (artwork.getVisibility() == View.GONE) {
            visibleLayout = actionsContainer;
            invisibleLayout = artwork;
        } else {
            visibleLayout = artwork;
            invisibleLayout = actionsContainer;
        }
        ObjectAnimator visToInvis = ObjectAnimator.ofFloat(visibleLayout, "rotationY", 0f, 90f);
        visToInvis.setDuration(TRANSITION_DURATION);
        visToInvis.setInterpolator(accelerator);
        final ObjectAnimator invisToVis = ObjectAnimator.ofFloat(invisibleLayout, "rotationY", -90f, 0f);
        invisToVis.setDuration(TRANSITION_DURATION);
        invisToVis.setInterpolator(decelerator);
        visToInvis.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                visibleLayout.setVisibility(View.GONE);
                invisToVis.start();
                invisibleLayout.setVisibility(View.VISIBLE);
            }
        });
        visToInvis.start();
    }
}
