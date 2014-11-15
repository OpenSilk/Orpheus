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
import android.os.Bundle;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.core.android.DrawerOwner;
import org.opensilk.music.ui2.loader.PluginLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action1;

public class Nav {

    @dagger.Module(
            injects = NavView.class,
            complete = false
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<NavView> {

        final EventBus bus;
        final DrawerOwner drawerOwner;
        final PluginLoader loader;

        Subscription subscription;

        @Inject
        public Presenter(@Named("activity") EventBus bus, DrawerOwner drawerOwner, PluginLoader loader) {
            this.bus = bus;
            this.drawerOwner = drawerOwner;
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            subscription = loader.getObservable().subscribe(new Action1<List<PluginInfo>>() {
                @Override
                public void call(List<PluginInfo> pluginInfos) {
                    if (getView() != null) getView().onLoad(pluginInfos);
                }
            });
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            if (subscription != null ) subscription.unsubscribe();
        }

        public void go(Context context, Screen screen) {
            if (screen == null) return;
            drawerOwner.closeDrawer();
            AppFlow.get(context).replaceTo(screen);
        }

        public void open(Object event) {
            drawerOwner.closeDrawer();
            bus.post(event);
        }
    }

}
