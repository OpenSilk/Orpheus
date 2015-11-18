package org.opensilk.common.ui.behavior;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import org.opensilk.common.core.util.VersionUtils;

/**
 * CoordinatorLayout Behavior for a quick return footer
 *
 * When a nested ScrollView is scrolled down, the quick return view will disappear.
 * When the ScrollView is scrolled back up, the quick return view will reappear.
 *
 * @author bherbst https://github.com/bherbst/QuickReturnFooterSample/
 */
//@SuppressWarnings("unused")
public class QuickReturnFooterBehavior extends CoordinatorLayout.Behavior<View> {
    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();

    private int mDySinceDirectionChange;
    private boolean hideRunning;
    private boolean showRunning;
    private ViewPropertyAnimatorCompat hideAnimator;
    private ViewPropertyAnimatorCompat showAnimator;

    public QuickReturnFooterBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mDySinceDirectionChange < 0 || dy < 0 && mDySinceDirectionChange > 0) {
            // We detected a direction change- cancel existing animations and reset our cumulative delta Y
            if (hideRunning && hideAnimator != null) {
                hideAnimator.cancel();
            } else if (showRunning && showAnimator != null) {
                showAnimator.cancel();
            }
            mDySinceDirectionChange = 0;
        }

        mDySinceDirectionChange += dy;

        if (mDySinceDirectionChange > child.getHeight() && !hideRunning) {
            hide(child);
        } else if (mDySinceDirectionChange < 0 && !showRunning) {
            show(child);
        }
    }

    /**
     * Hide the quick return view.
     *
     * Animates hiding the view, with the view sliding down and out of the screen.
     * After the view has disappeared, its visibility will change to GONE.
     *
     * @param view The quick return view
     */
    private void hide(final View view) {
        if (showRunning && showAnimator != null) {
            showAnimator.cancel();
        }
        ViewPropertyAnimatorCompat anim = ViewCompat.animate(view)
                .translationY(view.getHeight())
                .setInterpolator(INTERPOLATOR)
                .setDuration(200)
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {

                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        view.setVisibility(View.GONE);
                        hideRunning = false;
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                        hideRunning = false;
                    }
                });
        anim.start();
        hideRunning = true;
        hideAnimator = anim;
    }

    /**
     * Show the quick return view.
     *
     * Animates showing the view, with the view sliding up from the bottom of the screen.
     * After the view has reappeared, its visibility will change to VISIBLE.
     *
     * @param view The quick return view
     */
    private void show(final View view) {
        if (hideRunning && hideAnimator != null) {
            hideAnimator.cancel();
        }
        ViewPropertyAnimatorCompat anim = ViewCompat.animate(view)
                .translationY(0)
                .setInterpolator(INTERPOLATOR)
                .setDuration(200)
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        showRunning = false;
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                        showRunning = false;
                    }
                });
        anim.start();
        showRunning = true;
        showAnimator = anim;
    }
}