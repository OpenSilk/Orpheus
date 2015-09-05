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

package org.opensilk.common.ui.behavior;

/**
 * Created by drew on 9/4/15.
 */

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import java.util.List;

/**
 * Behavior designed for use with {@link FloatingActionButton} instances. It's main function
 * is to move {@link FloatingActionButton} views so that any displayed {@link Snackbar}s do
 * not cover them.
 */
public class SecondaryFabBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

    private Rect mTmpRect;

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent,
                                   FloatingActionButton child, View dependency) {
        // We're dependent on all SnackbarLayouts (if enabled)
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child,
                                          View dependency) {
//        if (dependency instanceof Snackbar.SnackbarLayout) {
//            updateFabTranslationForSnackbar(parent, child, dependency);
//        } else if (dependency instanceof AppBarLayout) {
//            // If we're depending on an AppBarLayout we will show/hide it automatically
//            // if the FAB is anchored to the AppBarLayout
//            updateFabVisibility(parent, (AppBarLayout) dependency, child);
//        }
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, FloatingActionButton child,
                                       View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            // If the removed view is a SnackbarLayout, we will animate back to our normal
            // position
            if (ViewCompat.getTranslationY(child) != 0f) {
                ViewCompat.animate(child)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                        .setListener(null);
            }
        }
    }

    private boolean updateFabVisibility(CoordinatorLayout parent,
                                        AppBarLayout appBarLayout, FloatingActionButton child) {
//        final CoordinatorLayout.LayoutParams lp =
//                (CoordinatorLayout.LayoutParams) child.getLayoutParams();
//        if (lp.getAnchorId() != appBarLayout.getId()) {
//            // The anchor ID doesn't match the dependency, so we won't automatically
//            // show/hide the FAB
//            return false;
//        }
//
//        if (mTmpRect == null) {
//            mTmpRect = new Rect();
//        }
//
//        // First, let's get the visible rect of the dependency
//        final Rect rect = mTmpRect;
//        getDescendantRect(parent, appBarLayout, rect);
//
//        if (rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {
//            // If the anchor's bottom is below the seam, we'll animate our FAB out
//            child.hide();
//        } else {
//            // Else, we'll animate our FAB back in
//            child.show();
//        }
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child,
                                 int layoutDirection) {
//        // First, lets make sure that the visibility of the FAB is consistent
//        final List<View> dependencies = parent.getDependencies(child);
//        for (int i = 0, count = dependencies.size(); i < count; i++) {
//            final View dependency = dependencies.get(i);
//            if (dependency instanceof AppBarLayout
//                    && updateFabVisibility(parent, (AppBarLayout) dependency, child)) {
//                break;
//            }
//        }
//        // Now let the CoordinatorLayout lay out the FAB
//        parent.onLayoutChild(child, layoutDirection);
//        // Now offset it if needed
//        offsetIfNeeded(parent, child);
        return true;
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    static void getDescendantRect(ViewGroup parent, View descendant, Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, out);
    }

    private static final ThreadLocal<Matrix> sMatrix = new ThreadLocal<>();
    private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();
    private static final Matrix IDENTITY = new Matrix();

    /**
     * This is a port of the common
     * {@link ViewGroup#offsetDescendantRectToMyCoords(android.view.View, android.graphics.Rect)}
     * from the framework, but adapted to take transformations into account. The result
     * will be the bounding rect of the real transformed rect.
     *
     * @param descendant view defining the original coordinate system of rect
     * @param rect (in/out) the rect to offset from descendant to this view's coordinate system
     */
    public static void offsetDescendantRect(ViewGroup group, View child, Rect rect) {
        Matrix m = sMatrix.get();
        if (m == null) {
            m = new Matrix();
            sMatrix.set(m);
        } else {
            m.set(IDENTITY);
        }

        offsetDescendantMatrix(group, child, m);

        RectF rectF = sRectF.get();
        if (rectF == null) {
            rectF = new RectF();
        }
        rectF.set(rect);
        m.mapRect(rectF);
        rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f),
                (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
    }

    static void offsetDescendantMatrix(ViewParent target, View view, Matrix m) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View && parent != target) {
            final View vp = (View) parent;
            offsetDescendantMatrix(target, vp, m);
            m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }

        m.preTranslate(view.getLeft(), view.getTop());

        if (!view.getMatrix().isIdentity()) {
            m.preConcat(view.getMatrix());
        }
    }
}