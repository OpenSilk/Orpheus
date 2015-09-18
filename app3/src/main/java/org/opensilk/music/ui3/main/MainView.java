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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.widget.FlingyFabLayout;
import org.opensilk.common.ui.widget.FloatingActionButton;
import org.opensilk.common.ui.widget.FloatingActionButtonCheckable;
import org.opensilk.music.R;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui.theme.PlaybackDrawableTint;
import org.opensilk.music.ui3.MusicActivityComponent;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.notSubscribed;

/**
 * Created by drew on 10/14/14.
 */
public class MainView extends FlingyFabLayout {

    MainPresenter presenter;
    PlaybackController playbackController;

    @InjectView(R.id.floating_action_button) FloatingActionButtonCheckable fabPlay;
    @InjectView(R.id.floating_action_next) FloatingActionButton fabNext;
    @InjectView(R.id.floating_action_prev) FloatingActionButton fabPrev;
    @InjectView(R.id.floating_action_shuffle) FloatingActionButton fabShuffle;
    @InjectView(R.id.floating_action_repeat) FloatingActionButton fabRepeat;

    AnimatorSet expandFabs;
    AnimatorSet collapseFabs;

    boolean secondaryFabsShowing = true;
    boolean firstLayout = true;

    public MainView(Context context, AttributeSet attrs) {
        super(makeContext(context), attrs);
        if (!isInEditMode()) {
            MusicActivityComponent component = DaggerService.getDaggerComponent(getContext());
            playbackController = component.playbackController();
            presenter = MortarScope.getScope(getContext()).getService(MainPresenter.class.getName());
        }
    }

    static Context makeContext(Context parent) {
        MortarScope scope = MortarScope.findChild(parent, MainView.class.getName());
        if (scope == null) {
            //were the root layout and have no screen or scope... so we make our own
            //the sole purpose of this is a slight hack to retain our presenter across
            //configuration changes
            MusicActivityComponent component = DaggerService.getDaggerComponent(parent);
            scope = MortarScope.getScope(parent).buildChild()
                    //presenter cant be scoped so we stash a copy in mortar so we can reuse it.
                    .withService(MainPresenter.class.getName(), component.mainPresenter())
                    .build(MainView.class.getName());
        } else {
            Timber.d("Reusing scope %s", MainView.class.getName());
        }
        return scope.createContext(parent);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        setupActionButton();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscribeFabClicks();
        if (!isInEditMode()) presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsubscribeFabClicks();
        if (!isInEditMode()) presenter.dropView(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Timber.v("onLayout firstLayout=%s", firstLayout);
        super.onLayout(changed, l, t, r, b);
        if (!firstLayout) return;
        firstLayout = false;
        // Now that everyone knows where they are supposed to be
        // move them into position for the expand animation
        fabNext.setY(fabPlay.getY());
        fabNext.setX(fabPlay.getX());
        fabNext.setVisibility(INVISIBLE);
        fabPrev.setY(fabPlay.getY());
        fabPrev.setX(fabPlay.getX());
        fabPrev.setVisibility(INVISIBLE);
        fabShuffle.setY(fabPlay.getY());
        fabShuffle.setX(fabPlay.getX());
        fabShuffle.setVisibility(INVISIBLE);
        fabRepeat.setY(fabPlay.getY());
        fabRepeat.setX(fabPlay.getX());
        fabRepeat.setVisibility(INVISIBLE);
        secondaryFabsShowing = false;
        setupFabAnimatiors();
    }

    @Override
    public boolean canCaptureView(View child, int pointerId) {
        if (child.getId() == fabPlay.getId()) {
            return true;
        } else if (fabNext.getVisibility() == VISIBLE && child.getId() == fabNext.getId()) {
            return true;
        } else if (fabPrev.getVisibility() == VISIBLE && child.getId() == fabPrev.getId()) {
            return true;
        } else if (fabRepeat.getVisibility() == VISIBLE && child.getId() == fabRepeat.getId()) {
            return true;
        } else if (fabShuffle.getVisibility() == VISIBLE && child.getId() == fabShuffle.getId()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onFabFling(View child, Direction direction) {
        if (child.getId() == fabPlay.getId()) {
            presenter.handlePrimaryFling();
        }
    }

    CompositeSubscription fabClicksSubscription;

    void subscribeFabClicks() {
        if (isSubscribed(fabClicksSubscription)) return;
        fabClicksSubscription = new CompositeSubscription(
                RxView.clickEvents(fabPlay).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        presenter.handlePrimaryClick();
                    }
                }),
                RxView.clickEvents(fabNext).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        playbackController.skipToNext();
                    }
                }),
                RxView.clickEvents(fabPrev).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        playbackController.skipToPrevious();
                    }
                }),
                RxView.clickEvents(fabShuffle).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        playbackController.shuffleQueue();
                    }
                }),
                RxView.clickEvents(fabRepeat).subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        playbackController.cycleRepeateMode();
                    }
                })
        );
    }

    void unsubscribeFabClicks() {
        if (notSubscribed(fabClicksSubscription)) return;
        fabClicksSubscription.unsubscribe();
        fabClicksSubscription = null;
    }

    void setupActionButton() {
        fabPlay.setOnDoubleClickListener(new FloatingActionButton.OnDoubleClickListener() {
            @Override
            public void onDoubleClick(View view) {
                presenter.handlePrimaryDoubleClick();
            }
        });
        fabPlay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                presenter.handlePrimaryLongClick();
                return true;
            }
        });
        fabPlay.bringToFront();

        PlaybackDrawableTint.shuffleDrawable24(fabShuffle);
        PlaybackDrawableTint.repeatDrawable24(fabRepeat);
    }

    void setupFabAnimatiors() {
        Interpolator overshootInterpolator = new OvershootInterpolator();
        Interpolator decelerateInterpolator = new DecelerateInterpolator();

        ObjectAnimator nextCY = ObjectAnimator.ofFloat(fabNext, "Y", fabPlay.getY());
        nextCY.setInterpolator(decelerateInterpolator);
        ObjectAnimator nextCX = ObjectAnimator.ofFloat(fabNext, "X", fabPlay.getX());
        nextCX.setInterpolator(decelerateInterpolator);
        ObjectAnimator prevCY = ObjectAnimator.ofFloat(fabPrev, "Y", fabPlay.getY());
        prevCY.setInterpolator(decelerateInterpolator);
        ObjectAnimator prevCX = ObjectAnimator.ofFloat(fabPrev, "X", fabPlay.getX());
        prevCX.setInterpolator(decelerateInterpolator);
        ObjectAnimator shuffleCY = ObjectAnimator.ofFloat(fabShuffle, "Y", fabPlay.getY());
        shuffleCY.setInterpolator(decelerateInterpolator);
        ObjectAnimator shuffleCX = ObjectAnimator.ofFloat(fabShuffle, "X", fabPlay.getX());
        shuffleCX.setInterpolator(decelerateInterpolator);
        ObjectAnimator repeatCY = ObjectAnimator.ofFloat(fabRepeat, "Y", fabPlay.getY());
        repeatCY.setInterpolator(decelerateInterpolator);
        ObjectAnimator repeatCX = ObjectAnimator.ofFloat(fabRepeat, "X", fabPlay.getX());
        repeatCX.setInterpolator(decelerateInterpolator);
        collapseFabs = new AnimatorSet();
        collapseFabs.playTogether(nextCY, nextCX, prevCY, prevCX, shuffleCY, shuffleCX, repeatCY, repeatCX);
        collapseFabs.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                fabNext.setVisibility(INVISIBLE);
                fabPrev.setVisibility(INVISIBLE);
                fabShuffle.setVisibility(INVISIBLE);
                fabRepeat.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        ObjectAnimator nextEY = ObjectAnimator.ofFloat(fabNext, "translationY", 0);
        nextEY.setInterpolator(overshootInterpolator);
        ObjectAnimator nextEX = ObjectAnimator.ofFloat(fabNext, "translationX", 0);
        nextEX.setInterpolator(overshootInterpolator);
        ObjectAnimator prevEY = ObjectAnimator.ofFloat(fabPrev, "translationY", 0);
        prevEY.setInterpolator(overshootInterpolator);
        ObjectAnimator prevEX = ObjectAnimator.ofFloat(fabPrev, "translationX", 0);
        prevEX.setInterpolator(overshootInterpolator);
        ObjectAnimator shuffleEY = ObjectAnimator.ofFloat(fabShuffle, "translationY", 0);
        shuffleEY.setInterpolator(overshootInterpolator);
        ObjectAnimator shuffleEX = ObjectAnimator.ofFloat(fabShuffle, "translationX", 0);
        shuffleEX.setInterpolator(overshootInterpolator);
        ObjectAnimator repeatEY = ObjectAnimator.ofFloat(fabRepeat, "translationY", 0);
        repeatEY.setInterpolator(overshootInterpolator);
        ObjectAnimator repeatEX = ObjectAnimator.ofFloat(fabRepeat, "translationX", 0);
        repeatEX.setInterpolator(overshootInterpolator);
        expandFabs = new AnimatorSet();
        expandFabs.playTogether(nextEY, nextEX, prevEY, prevEX, shuffleEY, shuffleEX, repeatEY, repeatEX);
        expandFabs.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                fabNext.setVisibility(VISIBLE);
                fabPrev.setVisibility(VISIBLE);
                fabShuffle.setVisibility(VISIBLE);
                fabRepeat.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    void toggleSecondaryFabs() {
        if (secondaryFabsShowing) {
            secondaryFabsShowing = false;
            expandFabs.cancel();
            collapseFabs.start();
        } else {
            secondaryFabsShowing = true;
            collapseFabs.cancel();
            expandFabs.start();
        }
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

}
