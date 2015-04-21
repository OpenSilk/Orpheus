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

package org.opensilk.common.mortarflow;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenContextFactory;
import org.opensilk.common.flow.ScreenSwitcherView;
import org.opensilk.common.impl.SimpleAnimationListener;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.common.util.ViewUtils;

import java.util.HashMap;

import flow.Flow;
import timber.log.Timber;

/**
 * Created by drew on 10/23/14.
 */
public class TransitionScreenSwitcher extends ScreenSwitcher {

    public static class Factory extends ScreenSwitcher.Factory {
        final ScreenContextFactory contextFactory;

        public Factory(int tagKey, ScreenContextFactory contextFactory) {
            super(tagKey);
            this.contextFactory = contextFactory;
        }

        @Override
        public ScreenSwitcher createScreenSwitcher(ScreenSwitcherView view) {
            return new TransitionScreenSwitcher(view, tagKey, contextFactory);
        }
    }

    private static class TransitionHolder {
        private final int[][] transitions;
        private TransitionHolder(int[][] transitions) {
            this.transitions = transitions;
        }
    }

    private static final HashMap<String, TransitionHolder> TRANSITIONS_CACHE = new HashMap<>();

    protected final ScreenContextFactory contextFactory;

    public TransitionScreenSwitcher(ScreenSwitcherView view, int tagKey, ScreenContextFactory contextFactory) {
        super(view, tagKey);
        this.contextFactory = contextFactory;
    }

    @Override
    protected void transition(final ViewGroup container, Screen from, Screen to, Flow.Direction direction, final Flow.Callback callback) {
        Timber.v("transition %s, %s, %s", (from != null ? from.getName() : null), to.getName(), direction);
        final Tag tag = ensureTag(container);
        tag.setNextScreen(to);

        final View oldChild = getCurrentChild();
        final View newChild = ViewUtils.inflate(contextFactory.setUpContext(to, container.getContext()), getLayout(to), container, false);

        switch (direction) {
            case FORWARD:
                if (from != null && oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    from.setViewState(ViewUtils.saveState(oldChild));
                    int[] transitions = getTransitions(to, WithTransitions.FORWARD);
                    if (transitions.length == 2) {
                        oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[1]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                    } else {
                        oldChild.setAnimation(null);
                        newChild.setAnimation(null);
                        callback.onComplete();
                    }
                    container.removeView(oldChild);
                    container.addView(newChild);
                } else {
                    int[] transitions = getTransitions(to, WithTransitions.SINGLE);
                    if (transitions.length == 1) {
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                        container.removeAllViews();
                        container.addView(newChild);
                    } else {
                        newChild.setAnimation(null);
                        container.removeAllViews();
                        container.addView(newChild);
                        callback.onComplete();
                    }
                }
                break;
            case BACKWARD:
                to.restoreHierarchyState(newChild);
                if (from != null && oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    int[] transitions = getTransitions(from, WithTransitions.BACKWARD);
                    if (transitions.length == 2) {
                        oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[1]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                    } else {
                        oldChild.setAnimation(null);
                        newChild.setAnimation(null);
                        callback.onComplete();
                    }
                    container.removeView(oldChild);
                    container.addView(newChild);
                } else {
                    int[] transitions = getTransitions(to, WithTransitions.SINGLE);
                    if (transitions.length == 1) {
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                        container.removeAllViews();
                        container.addView(newChild);
                    } else {
                        newChild.setAnimation(null);
                        container.removeAllViews();
                        container.addView(newChild);
                        callback.onComplete();
                    }
                }
                break;
            case REPLACE:
                to.restoreHierarchyState(newChild);
                if (oldChild != null) {
                    contextFactory.tearDownContext(oldChild.getContext());
                    int[] transitions = getTransitions(to, WithTransitions.REPLACE);
                    if (transitions.length == 2) {
                        oldChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[1]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                    } else {
                        oldChild.setAnimation(null);
                        newChild.setAnimation(null);
                        callback.onComplete();
                    }
                    container.removeAllViews();
                    container.addView(newChild);
                } else {
                    int[] transitions = getTransitions(to, WithTransitions.SINGLE);
                    if (transitions.length == 1) {
                        newChild.setAnimation(AnimationUtils.loadAnimation(container.getContext(), transitions[0]));
                        newChild.getAnimation().setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                        container.removeAllViews();
                        container.addView(newChild);
                    } else {
                        newChild.setAnimation(null);
                        container.removeAllViews();
                        container.addView(newChild);
                        callback.onComplete();
                    }
                }
                break;
        }
    }

    protected static int[] getTransitions(Object screen, int direction) {
        Class<Object> screenType = ObjectUtils.getClass(screen);
        TransitionHolder holder = TRANSITIONS_CACHE.get(screenType.getName());
        int[][] transitions;
        if (holder != null) {
            transitions = holder.transitions;
        } else {
            WithTransitions withTransitions = screenType.getAnnotation(WithTransitions.class);
            if (withTransitions == null) {
                Timber.w("@%s annotation not found on class %s",
                        WithTransitions.class.getSimpleName(), screenType.getName());
                return new int[0];
            }
            transitions = new int[4][];
            transitions[0] = withTransitions.single();
            transitions[1] = withTransitions.forward();
            transitions[2] = withTransitions.backward();
            transitions[3] = withTransitions.replace();
            TRANSITIONS_CACHE.put(screenType.getName(), new TransitionHolder(transitions));
        }
        int[] transition;
        switch (direction) {
            case WithTransitions.SINGLE:
                transition = transitions[0];
                if (transition.length == 1) return transition;
                else return new int[0];
            case WithTransitions.FORWARD:
                transition = transitions[1];
                if (transition.length == 2) return transition; break;
            case WithTransitions.BACKWARD:
                transition = transitions[2];
                if (transition.length == 2) return transition; break;
            case WithTransitions.REPLACE:
                transition = transitions[3];
                if (transition.length == 2) return transition; break;
            default:
                throw new IllegalArgumentException("Unknown transition direction: " + direction);
        }
        throw new IllegalArgumentException("Illegal array size for transition direction: " + direction);
    }

}
