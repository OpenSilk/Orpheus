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

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.andrew.apollo.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Flow;
import flow.Layouts;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
public class DrawerView extends DrawerLayout implements CanShowScreen<Blueprint> {

    @Inject
    GodScreen.Presenter presenter;

    @InjectView(R.id.drawer_container) ViewGroup navContainer;

    ScreenConductor<Blueprint> screenMaestro;
    ActionBarDrawerToggle drawerToggle;

    public DrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        screenMaestro = new ScreenConductor<>(getContext(),
                ButterKnife.<FrameLayout>findById(this, R.id.main));
        presenter.takeView(this);
        //add navlist
        ScreenConductor.addChild(getContext(), new NavScreen(), navContainer);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    public void showScreen(Blueprint screen, Flow.Direction direction) {
        if (screenMaestro != null) screenMaestro.showScreen(screen, direction);
    }

    public void setup() {
        setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        setupToggle();
    }

    public Flow getFlow() {
        return presenter.getFlow();
    }

    public boolean isDrawerOpen() {
        return navContainer != null && isDrawerOpen(navContainer);
    }

    public void closeDrawer() {
        if (isDrawerOpen()) closeDrawer(navContainer);
    }

    public void lockDrawer() {
        setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED, navContainer);
    }

    public void unlockDrawer() {
        setDrawerLockMode(LOCK_MODE_UNLOCKED, navContainer);
    }

    private void setupToggle() {
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        drawerToggle = new ActionBarDrawerToggle(
                (ActionBarActivity) getContext(),                    /* host Activity */
                this,                    /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                ((ActionBarActivity) getContext()).supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ((ActionBarActivity) getContext()).supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };
        setDrawerListener(drawerToggle);
        // Defer code dependent on restoration of previous instance state.
        post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });
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
            Timber.v("showScreen()");
            MortarScope myScope = Mortar.getScope(context);
            MortarScope newChildScope = myScope.requireChild(screen);

            View oldChild = getChildView();
            View newChild;

            if (oldChild != null) {
                MortarScope oldChildScope = Mortar.getScope(oldChild.getContext());
                if (oldChildScope.getName().equals(screen.getMortarScopeName())) {
                    // If it's already showing, short circuit.
                    Timber.v("Short circuit");
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
