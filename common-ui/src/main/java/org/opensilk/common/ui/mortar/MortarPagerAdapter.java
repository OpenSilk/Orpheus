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

package org.opensilk.common.ui.mortar;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.ui.util.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mortar.MortarScope;


/**
 * View pager adapter that uses mortar magick to take care of inflating,
 * scoping and saving views
 *
 * Screens should be annotated with @Layout and @WithComponent
 *
 * Created by drew on 11/16/14.
 */
public class MortarPagerAdapter<S extends Screen, V extends View> extends PagerAdapter {

    protected static final class Page<SS extends Screen, VV extends View> {
        public final SS screen;
        public final VV view;
        Page(SS screen, VV view) {
            this.screen = screen;
            this.view = view;
        }
    }

    protected final LayoutCreator layoutCreater;
    protected final MortarContextFactory contextFactory;
    protected final SparseArray<Page<S,V>> activePages = new SparseArray<>();
    protected Bundle savedState = new Bundle();

    protected final Context context;
    protected final ArrayList<S> screens;

    public MortarPagerAdapter(Context context) {
        this (context, new ArrayList<S>());
    }

    public MortarPagerAdapter(Context context, S[] screens) {
        this (context, Arrays.asList(screens));
    }

    public MortarPagerAdapter(@NonNull Context context, @NonNull List<S> screens) {
        this.context = context;
        MortarScope scope = MortarScope.getScope(context);
        if (scope.hasService(LayoutCreator.SERVICE_NAME)) {
            layoutCreater = scope.getService(LayoutCreator.SERVICE_NAME);
        } else {
            layoutCreater = new LayoutCreator();
        }
        if (scope.hasService(ScreenScoper.SERVICE_NAME)) {
            contextFactory = new MortarContextFactory(scope.<ScreenScoper>getService(ScreenScoper.SERVICE_NAME));
        } else {
            contextFactory = new MortarContextFactory();
        }
        this.screens = new ArrayList<>(screens);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        S screen = screens.get(position);
        Context newChildContext = decorateContext(contextFactory.setUpContext(screen, context), position);
        V newChild = ViewUtils.inflate(newChildContext, layoutCreater.getLayout(screens.get(position)), container, false);
        ViewUtils.restoreState(newChild, savedState, screen.getName());
        container.addView(newChild);
        Page<S,V> newPage = new Page<S,V>(screen, newChild);
        activePages.put(position, newPage);
        return newPage;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object o) {
        Page oldPage = (Page) o;
        activePages.remove(position);
        ViewUtils.saveState(oldPage.view, savedState, oldPage.screen.getName());
        contextFactory.tearDownContext(oldPage.view.getContext());
        container.removeView(oldPage.view);
    }

    @Override
    public int getCount() {
        return screens.size();
    }

    @Override
    public boolean isViewFromObject(android.view.View view, Object o) {
        return view == ((Page) o).view;
    }

    @Override
    public Parcelable saveState() {
        final int size = activePages.size();
        for (int ii=0; ii<size; ii++) {
            Page<S,V> p = activePages.valueAt(ii);
            ViewUtils.saveState(p.view, savedState, p.screen.getName());
        }
        return savedState;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state == null || !(state instanceof Bundle)) return;
        Bundle b = (Bundle) state;
        b.setClassLoader(loader);
        savedState = b;
    }

    /**
     * @return Modifiable list of screens, be sure to call
     *         {@link #notifyDataSetChanged()} to propagate any changes
     */
    public List<S> screens() {
        return screens;
    }

    /** Allows themeing views */
    protected Context decorateContext(Context newChildContext, int position) {
        return newChildContext;
    }

}
