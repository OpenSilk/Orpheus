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

package org.opensilk.common.dagger;

import android.app.Application;

import dagger.ObjectGraph;

/**
 * Created by drew on 5/31/14.
 */
public abstract class DaggerApplication extends Application implements DaggerInjector {

    protected ObjectGraph mGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        mGraph = ObjectGraph.create(getModules());
    }

    @Override
    public void inject(Object obj) {
        mGraph.inject(obj);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mGraph;
    }

    protected abstract Object[] getModules();

}