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

package org.opensilk.music.ui3.library;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import org.opensilk.music.ui3.main.FooterScreenView;

import java.util.List;

/**
 * Created by drew on 10/8/15.
 */
public class FabFooterBehavior extends FloatingActionButton.Behavior {

    public FabFooterBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof FooterScreenView || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof FooterScreenView) {
            updateFabTranslationForFooter(parent, child, dependency);
        } else {
            super.onDependentViewChanged(parent, child, dependency);
        }
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        super.onDependentViewRemoved(parent, child, dependency);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child, int layoutDirection) {
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    private void updateFabTranslationForFooter(CoordinatorLayout parent, FloatingActionButton fab, View footer) {
        if (footer.getVisibility() == View.VISIBLE) {
            fab.show();
            float translationY = this.getFabTranslationYForFooter(parent, fab);
            ViewCompat.setTranslationY(fab, translationY);
        } else {
            fab.hide();
        }
    }

    private float getFabTranslationYForFooter(CoordinatorLayout parent, FloatingActionButton fab) {
        float minOffset = 0.0F;
        List dependencies = parent.getDependencies(fab);
        int i = 0;

        for(int z = dependencies.size(); i < z; ++i) {
            View view = (View)dependencies.get(i);
            if(view instanceof FooterScreenView && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float)view.getHeight());
            }
        }

        return minOffset;
    }
}
