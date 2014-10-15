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

package org.opensilk.music.ui2.main;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.andrew.apollo.R;

import org.opensilk.music.widgets.FloatingActionButton;
import org.opensilk.music.widgets.FloatingActionButtonRelativeLayout;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/14/14.
 */
public class FloatingActionContainer extends FloatingActionButtonRelativeLayout {

    @InjectView(R.id.floating_action_button)
    FloatingActionButton fabPlay;
    @InjectView(R.id.floating_action_next)
    FloatingActionButton fabNext;
    @InjectView(R.id.floating_action_prev)
    FloatingActionButton fabPrev;
    @InjectView(R.id.floating_action_shuffle)
    FloatingActionButton fabShuffle;
    @InjectView(R.id.floating_action_repeat)
    FloatingActionButton fabRepeat;

    boolean secondaryFabsShowing = false;

    public FloatingActionContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        setupActionButton();
        setupLayoutTransitions();
    }

    void setupActionButton() {
        fabPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        fabPlay.setOnDoubleClickListener(new FloatingActionButton.OnDoubleClickListener() {
            @Override
            public void onDoubleClick(View view) {
                toggleSecondaryFabs();
            }
        });
        fabPlay.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleSecondaryFabs();
                return true;
            }
        });
    }

    void setupLayoutTransitions() {
        LayoutTransition lt = new LayoutTransition();
//        lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
//        lt.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        Animator appearing = AnimatorInflater.loadAnimator(getContext(), R.animator.fab_slide_in_left);
        lt.setAnimator(LayoutTransition.APPEARING, appearing);
        Animator disappearing = AnimatorInflater.loadAnimator(getContext(), R.animator.fab_slide_out_left);
        lt.setAnimator(LayoutTransition.DISAPPEARING, disappearing);
        setLayoutTransition(lt);
    }

    void toggleSecondaryFabs() {
        if (secondaryFabsShowing) {
            fabNext.setVisibility(GONE);
            fabPrev.setVisibility(GONE);
            fabShuffle.setVisibility(GONE);
            fabRepeat.setVisibility(GONE);
            secondaryFabsShowing = false;
        } else {
            fabNext.setVisibility(VISIBLE);
            fabPrev.setVisibility(VISIBLE);
            fabShuffle.setVisibility(VISIBLE);
            fabRepeat.setVisibility(VISIBLE);
            secondaryFabsShowing = true;
        }
    }

}
