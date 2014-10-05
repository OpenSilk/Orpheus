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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 10/3/14.
 */
public class GodView extends DrawerLayout implements CanShowScreen<Blueprint> {

    @Inject
    GodScreen.Presenter presenter;

    ScreenConductor<Blueprint> screenMaestro;

    public GodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        screenMaestro = new ScreenConductor<>(getContext(),
                ButterKnife.<FrameLayout>findById(this, R.id.main));
        presenter.takeView(this);
        addDrawerView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public Flow getFlow() {
        return presenter.getFlow();
    }

    @Override
    public void showScreen(Blueprint screen, Flow.Direction direction) {
        if (screenMaestro != null) screenMaestro.showScreen(screen, direction);
    }

    public void addDrawerView() {
        ScreenConductor.addChild(getContext(), new DrawerScreen(), ButterKnife.<ViewGroup>findById(this, R.id.drawer_container));
    }

    /**
     * A conductor that can swap subviews within a container view.
     * <p/>
     *
     * @param <S> the type of the screens that serve as a {@link Blueprint} for subview. Must
     * be annotated with {@link flow.Layout}, suitable for use with {@link flow.Layouts#createView}.
     */
    public static class ScreenConductor<S extends Blueprint> implements CanShowScreen<S> {

        private final Context context;
        private final ViewGroup container;

        /**
         * @param container the container used to host child views. Typically this is a {@link
         * android.widget.FrameLayout} under the action bar.
         */
        public ScreenConductor(Context context, ViewGroup container) {
            this.context = context;
            this.container = container;
        }

        public static void addChild(Context context, Blueprint screen, ViewGroup parent) {
            MortarScope myScope = Mortar.getScope(context);
            MortarScope newChildScope = myScope.requireChild(screen);
            View newChild = Layouts.createView(newChildScope.createContext(context), screen);
            parent.addView(newChild);
        }

        public void showScreen(S screen, Flow.Direction direction) {
            MortarScope myScope = Mortar.getScope(context);
            MortarScope newChildScope = myScope.requireChild(screen);

            View oldChild = getChildView();
            View newChild;

            if (oldChild != null) {
                MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
                if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
                    // If it's already showing, short circuit.
                    return;
                }

                myScope.destroyChild(oldChildScope);
            }

            // Create the new child.
            Context childContext = newChildScope.createContext(context);
            newChild = Layouts.createView(childContext, screen);

            setAnimation(direction, oldChild, newChild);

            // Out with the old, in with the new.
            if (oldChild != null) container.removeView(oldChild);
            container.addView(newChild);
        }

        protected void setAnimation(Flow.Direction direction, View oldChild, View newChild) {
//        if (oldChild == null) return;

//        int out = direction == Flow.Direction.FORWARD ? R.anim.slide_out_left : R.anim.slide_out_right;
//        int in = direction == Flow.Direction.FORWARD ? R.anim.slide_in_right : R.anim.slide_in_left;
//
//        oldChild.setAnimation(loadAnimation(context, out));
//        newChild.setAnimation(loadAnimation(context, in));
        }

        private View getChildView() {
            return container.getChildAt(0);
        }
    }

}
