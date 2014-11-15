/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music;

import android.content.Context;

import org.opensilk.music.artwork.ArtworkModule;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * Created by drew on 6/21/14.
 */
public class GraphHolder implements DaggerInjector {

    private static GraphHolder instance;

    public static synchronized GraphHolder get(Context context) {
        if (instance == null) {
            instance = new GraphHolder(context);
        }
        return instance;
    }

    private ObjectGraph graph;

    private GraphHolder(Context context) {
        this.graph = ObjectGraph.create(new GlobalModule(context));
    }

    @Override
    public void inject(Object o) {
        graph.inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return graph;
    }

    public <T> T getObj(Class<T> cls) {
        return getObjectGraph().get(cls);
    }
}
