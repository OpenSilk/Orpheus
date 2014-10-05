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

import android.os.Bundle;

import org.opensilk.music.R;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

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
public class DrawerScreen implements Blueprint {

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
            injects = DrawerView.class
    )
    public static class Module {

    }

    public static class Presenter extends ViewPresenter<DrawerView> {

        private static List<String> sItems = Arrays.asList(
                "Gallery",
                "Folders"
        );

        final Flow flow;

        @Inject
        public Presenter(Flow flow) {
            this.flow = flow;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            getView().setup(sItems);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        public void go(int pos) {
            switch (pos) {
                case 0:
                    flow.replaceTo(new GalleryScreen());
                    break;
                case 1:
                    break;
            }
        }
    }
}
