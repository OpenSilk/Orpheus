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

package org.opensilk.music.ui2.main2;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Parcelable;
import android.transition.Transition;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenContextFactory;
import org.opensilk.common.flow.WithTransition;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.R;
import org.opensilk.music.ui2.main.QueueScreen;
import org.opensilk.music.ui2.main2.ScreenSwitcher;
import org.opensilk.common.flow.ScreenSwitcherView;
import org.opensilk.music.ui2.util.ViewStateSaver;

import java.util.LinkedHashMap;
import java.util.Map;

import flow.Flow;
import flow.Layout;
import hugo.weaving.DebugLog;
import timber.log.Timber;

import static flow.Flow.Direction.FORWARD;
import static flow.Flow.Direction.REPLACE;
import static org.opensilk.common.util.Preconditions.checkNotNull;

/**
 * Created by drew on 10/23/14.
 */
public class ScreenConductor extends ScreenSwitcher {

    public static class Factory extends ScreenSwitcher.Factory {
        final ScreenContextFactory contextFactory;

        public Factory(int tagKey, ScreenContextFactory contextFactory) {
            super(tagKey);
            this.contextFactory = contextFactory;
        }

        @Override
        public ScreenSwitcher createScreenSwitcher(ScreenSwitcherView view) {
            return new ScreenConductor(view, tagKey, contextFactory);
        }
    }

    final ScreenContextFactory contextFactory;

    public ScreenConductor(ScreenSwitcherView view, int tagKey, ScreenContextFactory contextFactory) {
        super(view, tagKey);
        this.contextFactory = contextFactory;
    }

    @Override
    protected void transition(final ViewGroup container, Screen from, Screen to, Flow.Direction direction, final Flow.Callback callback) {
        Timber.v("transition %s, %s, %s", (from != null ? from.getName() : null), to.getName(), direction);
        final Tag tag = ensureTag(container);
        tag.setNextScreen(to);

        final View oldChild = getCurrentChild();
        final View newChild = ViewStateSaver.inflate(contextFactory.setUpContext(to, container.getContext()), getLayout(to), container, false);

        switch (direction) {
            case FORWARD:
                if (from != null && oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    from.setViewState(ViewStateSaver.save(oldChild));
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.shrink_fade_out));
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_in_child_bottom));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeView(oldChild);
                    container.addView(newChild);
                } else {
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeAllViews();
                    container.addView(newChild);
                }
                break;
            case BACKWARD:
                to.restoreHierarchyState(newChild);
                if (oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_out_child_bottom));
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeView(oldChild);
                    container.addView(newChild);
                } else {
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeAllViews();
                    container.addView(newChild);
                }
                break;
            case REPLACE:
                to.restoreHierarchyState(newChild);
                if (oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.shrink_fade_out));
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_in_left));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeAllViews();
                    container.addView(newChild);
                } else {
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                    newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onComplete();
                        }
                    });
                    container.removeAllViews();
                    container.addView(newChild);
                }
                break;
        }
    }

    protected static int getTransition(Object screen, int direction) {
        Class<Object> screenType = ObjectUtils.getClass(screen);
        WithTransition transitions = screenType.getAnnotation(WithTransition.class);
        checkNotNull(transitions, "@%s annotation not found on class %s",
                WithTransition.class.getSimpleName(), screenType.getName());
        return direction == WithTransition.IN ? transitions.in() : transitions.out();
    }

    protected static class SimpleAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

}
