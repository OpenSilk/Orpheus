/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.dream.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.TextView;

import org.opensilk.music.R;
import org.opensilk.music.MusicServiceConnection;

import javax.inject.Inject;

import butterknife.InjectView;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 4/13/14.
 */
public class ArtWithControls extends ArtOnly {

    private static final int[] STATE_CHECKED = new int[]{android.R.attr.state_checked};
    private static final int[] STATE_UNCHECKED = new int[]{};

    @Inject MusicServiceConnection mServiceConnection;

    @InjectView(R.id.track_title) TextView mTrackTitle;
    @InjectView(R.id.dream_action_play) ImageButton mPlayPauseButton;
    @InjectView(R.id.dream_action_prev) ImageButton mPrevButton;
    @InjectView(R.id.dream_action_next) ImageButton mNextButton;

    CompositeSubscription subscriptions;

    public ArtWithControls(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscriptions = new CompositeSubscription(
                ViewObservable.clicks(mPlayPauseButton).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        mServiceConnection.playOrPause();
                    }
                }),
                ViewObservable.clicks(mPrevButton).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        mServiceConnection.prev();
                    }
                }),
                ViewObservable.clicks(mNextButton).subscribe(new Action1<OnClickEvent>() {
                    @Override
                    public void call(OnClickEvent onClickEvent) {
                        mServiceConnection.next();
                    }
                })
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (subscriptions != null) {
            subscriptions.unsubscribe();
            subscriptions = null;
        }
    }

    @Override
    public void updatePlaystate(boolean playing) {
        mPlayPauseButton.setImageState(playing ? STATE_CHECKED : STATE_UNCHECKED, true);
    }

    @Override
    public void updateTrack(String name) {
        mTrackTitle.setText(name);
    }

}
