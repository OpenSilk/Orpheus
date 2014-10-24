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

        final View oldChild = getCurrentChild();
        View newChild = ViewStateSaver.inflate(contextFactory.setUpContext(to, container.getContext()), getLayout(to), container, false);

        switch (direction) {
            case FORWARD:
                if (from != null && oldChild != null) {
                    //save the oldchilds view state
                    SparseArray<Parcelable> state = new SparseArray<>();
                    oldChild.saveHierarchyState(state);
                    from.setViewState(state);
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.shrink_fade_out));
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_in_child_bottom));
                } else {
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                }
                break;
            case BACKWARD:
                if (from != null) {
                    from.restoreHierarchyState(newChild);
                }
                if (oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_out_child_bottom));
                }
                newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
//                container.addView(newChild);
                break;
            case REPLACE:
//                container.removeAllViews();
//                container.addView(newChild);
//                callback.onComplete();
                if (oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.shrink_fade_out));
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.slide_in_left));
                } else {
                    newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), R.anim.grow_fade_in));
                }
                break;
        }
        tag.setNextScreen(to);
        newChild.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationEnd(Animation animation) {
                callback.onComplete();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        if (oldChild != null) container.removeView(oldChild);
        container.addView(newChild);
    }

    protected AnimatorSet animateSwitch(final View from, int fromTransition, View to, int toTransition) {
        Animator out = loadAnimator(from.getContext(), fromTransition);
        Animator in = loadAnimator(to.getContext(), toTransition);
        out.setTarget(from);
        in.setTarget(to);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(out, in);
        return set;
    }

    private static Animator loadAnimator(Context context, int animationId) {
        return AnimatorInflater.loadAnimator(context, animationId);
    }

    protected static int getTransition(Object screen, int direction) {
        Class<Object> screenType = ObjectUtils.getClass(screen);
        WithTransition transitions = screenType.getAnnotation(WithTransition.class);
        checkNotNull(transitions, "@%s annotation not found on class %s",
                WithTransition.class.getSimpleName(), screenType.getName());
        return direction == WithTransition.IN ? transitions.in() : transitions.out();
    }

    /*
    @Override protected void transition(final ViewGroup container, Screen from, Screen to,
                                        final Flow.Direction direction, final Flow.Callback callback) {
        final Tag tag = ensureTag(container);
        final PathContext context;
        final PathContext oldPath;
        if (container.getChildCount() > 0) {
            oldPath = PathContext.get(container.getChildAt(0).getContext());
        } else {
            oldPath = PathContext.empty(container.getContext());
        }

        ViewGroup view;
        context = PathContext.create(oldPath, to, contextFactory);
        int layout = getLayout(to);
        view = (ViewGroup) LayoutInflater.from(context)
                .cloneInContext(context)
                .inflate(layout, container, false);

        View fromView = null;
        tag.setNextScreen(to);
        if (tag.fromScreen != null) {
            fromView = container.getChildAt(0);
            SparseArray<Parcelable> state = new SparseArray<>();
            fromView.saveHierarchyState(state);
            tag.fromScreen.setViewState(state);
        }

        if (fromView == null || direction == REPLACE) {
            container.removeAllViews();
            container.addView(view);
            tag.toScreen.restoreHierarchyState(container.getChildAt(0));
            oldPath.destroyNotIn(context, contextFactory);
            callback.onComplete();
        } else {
            container.addView(view);
            final View finalFromView = fromView;
            Utils.waitForMeasure(view, new Utils.OnMeasuredCallback() {
                @Override public void onMeasured(View view, int width, int height) {
                    runAnimation(container, finalFromView, view, direction, new Flow.Callback() {
                        @Override public void onComplete() {
                            container.removeView(finalFromView);
                            tag.toScreen.restoreHierarchyState(container.getChildAt(0));
                            oldPath.destroyNotIn(context, contextFactory);
                            callback.onComplete();
                        }
                    });
                }
            });
        }
    }

    private void runAnimation(final ViewGroup container, final View from, final View to,
                              Flow.Direction direction, final Flow.Callback callback) {
        Animator animator = createSegue(from, to, direction);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                container.removeView(from);
                callback.onComplete();
            }
        });
        animator.start();
    }

    private Animator createSegue(View from, View to, Flow.Direction direction) {
        boolean backward = direction == Flow.Direction.BACKWARD;
        int fromTranslation = backward ? from.getWidth() : -from.getWidth();
        int toTranslation = backward ? -to.getWidth() : to.getWidth();

        AnimatorSet set = new AnimatorSet();

        set.play(ObjectAnimator.ofFloat(from, View.TRANSLATION_X, fromTranslation));
        set.play(ObjectAnimator.ofFloat(to, View.TRANSLATION_X, toTranslation, 0));

        return set;
    }
    */
}
