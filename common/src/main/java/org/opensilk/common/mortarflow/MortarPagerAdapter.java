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

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.common.util.ViewUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import flow.Layout;

import static org.opensilk.common.util.Preconditions.checkNotNull;

/**
 * View pager adapter that uses mortar magick to take care of inflating,
 * scoping and saving views
 *
 * Screens should be annotated with @Layout and @WithModule
 *
 * Created by drew on 11/16/14.
 */
public class MortarPagerAdapter<S extends Screen, V extends View> extends PagerAdapter {

    protected final class Page {
        public final S screen;
        public final V view;
        Page(S screen, V view) {
            this.screen = screen;
            this.view = view;
        }
    }

    protected final Map<Class, Integer> screenLayoutCache = new LinkedHashMap<>();
    protected final MortarContextFactory contextFactory = new MortarContextFactory();
    protected final Map<Integer, Page> activePages = new LinkedHashMap<>();
    protected Bundle savedState = new Bundle();

    protected final Context context;
    protected final List<S> screens;

    public MortarPagerAdapter(Context context, S[] screens) {
        this (context, Arrays.asList(screens));
    }

    public MortarPagerAdapter(Context context, List<S> screens) {
        this.context = context;
        this.screens = screens;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        S screen = screens.get(position);
        Context newChildContext = decorateContext(contextFactory.setUpContext(screen, context), position);
        V newChild = ViewUtils.inflate(newChildContext, getLayout(position), container, false);
        ViewUtils.restoreState(newChild, savedState, screen.getName());
        container.addView(newChild);
        Page newPage = new Page(screen, newChild);
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
        for (Map.Entry<Integer, Page> entry : activePages.entrySet()) {
            ViewUtils.saveState(entry.getValue().view, savedState, entry.getValue().screen.getName());
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

    /** Allows themeing views */
    protected Context decorateContext(Context newChildContext, int position) {
        return newChildContext;
    }

    protected int getLayout(int position) {
        S screen = screens.get(position);
        Class<? extends S> screenType = ObjectUtils.getClass(screen);
        Integer layoutResId = screenLayoutCache.get(screenType);
        if (layoutResId == null) {
            Layout layout = screenType.getAnnotation(Layout.class);
            checkNotNull(layout, "@%s annotation not found on class %s", Layout.class.getSimpleName(),
                    screenType.getName());
            layoutResId = layout.value();
            screenLayoutCache.put(screenType, layoutResId);
        }
        return layoutResId;
    }

}
