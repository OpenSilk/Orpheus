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

package org.opensilk.music;

import org.robolectric.Robolectric;

import dagger.ObjectGraph;

/**
 * Created by drew on 10/17/14.
 */
public class TestMusicApp extends MusicApp {

    @Override
    protected void enableStrictMode() {
        //Stub out
    }

    @Override
    protected void setupDagger() {
        // for tests we dont use the GraphHolder since i cannot think
        // of anyway to override that, so we just make our own global module
        // and ignore the graph holder global module
        ObjectGraph og = ObjectGraph.create(new TestGlobalModule(getApplicationContext()));
        mScopedGraphe = og.plus(new AppModule(this));
    }

    public static void injectTest(Object obj) {
        TestMusicApp app = (TestMusicApp) Robolectric.application;
        app.inject(obj);
    }
}
