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
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.widget.ImageButtonCheckable;
import org.opensilk.common.ui.widget.ImageButtonTriState;
import org.opensilk.music.R;
import org.opensilk.music.playback.PlaybackConstants;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 9/19/15.
 */
public class ControlsScreenView extends RelativeLayout {

    @Inject ControlsScreenPresenter mPresenter;

    @InjectView(R.id.now_playing_seekprogress) CircularSeekBar seekBar;
    @InjectView(R.id.now_playing_current_time) TextView currentTime;
    @InjectView(R.id.now_playing_total_time) TextView totalTime;
    @InjectView(R.id.now_playing_shuffle) ImageButtonCheckable shuffle;
    @InjectView(R.id.now_playing_previous) ImageButton prev;
    @InjectView(R.id.now_playing_play) ImageButtonCheckable play;
    @InjectView(R.id.now_playing_next) ImageButton next;
    @InjectView(R.id.now_playing_repeat) ImageButtonTriState repeat;

    CompositeSubscription clicks;

    public ControlsScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            ControlsScreenComponent cmpnt = DaggerService.getDaggerComponent(getContext());
            cmpnt.inject(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            mPresenter.takeView(this);
            subscribeClicks();
            setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeClicks();
        mPresenter.dropView(this);
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

    public void setPlayChecked(boolean yes) {
        play.setChecked(yes);
    }

    public void setProgress(int progress) {
        if (progress < 0 || progress > 1000) {
            //were in a loading state disable seeking
            seekBar.setIsTouchEnabled(false);
            seekBar.setProgress(1000);
        } else {
            seekBar.setIsTouchEnabled(true);
            seekBar.setProgress(progress);
        }
    }

    @DebugLog
    public void setRepeatLevel(int mode) {
        repeat.setState(mode);
    }

    public void setShuffleLevel(int mode) {
        shuffle.setChecked(mode == PlaybackConstants.SHUFFLE_NORMAL);
    }

    void setup() {
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                mPresenter.onProgressChanged(progress, fromUser);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                mPresenter.onStopTrackingTouch();
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                mPresenter.onStartTrackingTouch();
            }
        });
        if (VersionUtils.hasLollipop()) {
            //add state transitions
            AnimatedStateListDrawable drawable = (AnimatedStateListDrawable) play.getDrawable();
            drawable.addTransition(R.id.pause_state, R.id.play_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_pause_play_black_animated_36dp), false);
            drawable.addTransition(R.id.play_state, R.id.pause_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_play_pause_black_animated_36dp), false);

            AnimatedStateListDrawable repeatDrawable = (AnimatedStateListDrawable) repeat.getDrawable();
            repeatDrawable.addTransition(R.id.repeat_off_state, R.id.repeat_on_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_repeat_off_on_black_animated_36dp), true);
            repeatDrawable.addTransition(R.id.repeat_on_state, R.id.repeat_one_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_repeat_on_one_black_animated_36dp), true);
            repeatDrawable.addTransition(R.id.repeat_one_state, R.id.repeat_off_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_repeat_one_off_black_animated_36dp), true);
            repeatDrawable.addTransition(R.id.repeat_off_state, R.id.repeat_one_state, (AnimatedVectorDrawable)
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_repeat_off_one_black_animated_36dp), true);
        }
    }

    void subscribeClicks() {
        if (isSubscribed(clicks)) return;
        clicks = new CompositeSubscription(
                RxView.clickEvents(shuffle).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        mPresenter.getPlaybackController().shuffleQueue();
                    }
                }),
                RxView.clickEvents(prev).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        mPresenter.getPlaybackController().skipToPrevious();
                    }
                }),
                RxView.clickEvents(play).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        mPresenter.getPlaybackController().playorPause();
                    }
                }),
                RxView.clickEvents(next).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        mPresenter.getPlaybackController().skipToNext();
                    }
                }),
                RxView.clickEvents(repeat).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        mPresenter.getPlaybackController().cycleRepeateMode();
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
}
