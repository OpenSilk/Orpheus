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
import android.os.Bundle;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.loader.LoaderCallback;
import org.opensilk.music.loader.NavLoader;

import java.util.List;

import javax.inject.Inject;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import mortar.Blueprint;
import mortar.ViewPresenter;

/**
 * Note to self: we arent embedding this listview in order to obtain the GodView's flow
 *      so we make sure to only add this screen after the GodView is inflated
 *
 * Created by drew on 10/4/14.
 */
@Layout(R.layout.drawer_list)
public class NavScreen implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            addsTo = GodScreen.Module.class,
            injects = NavView.class
    )
    public static class Module {

    }

    public static class Presenter extends ViewPresenter<NavView> implements LoaderCallback<PluginInfo> {

        final Flow flow;
        final NavLoader loader;

        @Inject
        public Presenter(Flow flow, NavLoader loader) {
            this.flow = flow;
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup();
            loader.loadAsync(this);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        @Override
        public void onLoadComplete(List<PluginInfo> items) {
            NavView v = getView();
            if (v != null) v.getAdapter().load(items);
        }

        public void go(Blueprint screen) {
            flow.replaceTo(screen);
        }

        public void openSettings(Context context) {
            NavUtils.openSettings((Activity)context);
        }
    }
}
