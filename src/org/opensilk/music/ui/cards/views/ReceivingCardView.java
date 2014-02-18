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

package org.opensilk.music.ui.cards.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.ui.cards.CardBaseList;
import org.opensilk.music.widgets.NowPlayingAnimation;

import it.gmariotti.cardslib.library.view.CardView;

/**
 * CardView that receives broadcasts
 *
 * Created by drew on 2/18/14.
 */
public class ReceivingCardView extends CardView {
    public ReceivingCardView(Context context) {
        super(context);
    }

    public ReceivingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReceivingCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        getContext().registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mReceiver);
    }

    /**
     * Listens for playstate changes and starts the now playing animition
     * on list cards
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicPlaybackService.PLAYSTATE_CHANGED.equals(intent.getAction())
                    || MusicPlaybackService.META_CHANGED.equals(intent.getAction())) {
                //noinspection ConstantConditions
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        final NowPlayingAnimation animation = ((CardBaseList) getCard()).getAnimation();
                        if (animation != null) {
                            long trackId = MusicUtils.getCurrentAudioId();
                            if (MusicUtils.isPlaying() && ((CardBaseList) getCard()).shouldStartAnimating(trackId)) {
                                animation.startAnimating(trackId);
                            } else {
                                if (animation.isAnimating()) {
                                    animation.stopAnimating();
                                }
                            }
                        }
                    }
                });
            }
        }
    };
}
