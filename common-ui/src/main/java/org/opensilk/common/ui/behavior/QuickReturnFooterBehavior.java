package org.opensilk.common.ui.behavior;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

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

    public QuickReturnFooterBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mDySinceDirectionChange < 0
                || dy < 0 && mDySinceDirectionChange > 0) {
            // We detected a direction change- cancel existing animations and reset our cumulative delta Y
            child.animate().cancel();
            mDySinceDirectionChange = 0;
        }

        mDySinceDirectionChange += dy;

        if (mDySinceDirectionChange > child.getHeight() && child.getVisibility() == View.VISIBLE) {
            hide(child);
        } else if (mDySinceDirectionChange < 0 && child.getVisibility() == View.GONE) {
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
        ViewCompat.animate(view)
                .translationY(view.getHeight())
                .setInterpolator(INTERPOLATOR)
                .withLayer()
                .setDuration(200)
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(View view) {
                        // Canceling a hide should show the view
                        show(view);
                    }
                })
                .start();
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
        ViewCompat.animate(view)
                .translationY(0)
                .setInterpolator(INTERPOLATOR)
                .setDuration(200)
                .withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(View view) {

                    }

                    @Override
                    public void onAnimationCancel(View view) {
                        // Canceling a show should hide the view
                        hide(view);
                    }
                })
                .start();
    }
}