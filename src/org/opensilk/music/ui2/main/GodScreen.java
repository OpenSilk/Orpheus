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

import org.opensilk.music.AppModule;
import com.andrew.apollo.R;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Flow;
import flow.Layout;
import flow.Parcer;
import mortar.Blueprint;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.activity_god)
public class GodScreen implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module(
            addsTo = AppModule.class,
            injects = {
                    DrawerView.class
            },
            library = true
    )
    public static class Module {

        @Provides
        public Flow provideFlow(Presenter presenter) {
            return presenter.getFlow();
        }

    }

    @Singleton
    public static class Presenter extends FlowControl<Blueprint, DrawerView> {

        @Inject
        Presenter(Parcer<Object> flowParcer) {
            super(flowParcer);
        }

        @Override
        public void showScreen(Blueprint newScreen, Flow.Direction direction) {
            super.showScreen(newScreen, direction);
        }

        @Override
        protected Blueprint getFirstScreen() {
            return new GalleryScreen();
        }
    }

}
