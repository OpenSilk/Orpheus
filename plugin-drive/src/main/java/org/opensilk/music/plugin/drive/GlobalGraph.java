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

package org.opensilk.music.plugin.drive;

import android.content.Context;

import org.opensilk.common.dagger.AppContextModule;
import org.opensilk.common.dagger.DaggerInjector;

import dagger.ObjectGraph;

/**
 * Created by drew on 4/28/15.
 */
public class GlobalGraph implements DaggerInjector {

    private static GlobalGraph instance;

    private final ObjectGraph graph;

    public GlobalGraph(Context appContext) {
        this.graph = ObjectGraph.create(new AppContextModule(appContext)).plus(new GlobalModule());
    }

    public static GlobalGraph get(Context context) {
        if (instance == null) {
            instance = new GlobalGraph(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void inject(Object obj) {
        graph.inject(obj);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return graph;
    }
}
