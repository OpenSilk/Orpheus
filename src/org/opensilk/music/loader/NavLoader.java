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

package org.opensilk.music.loader;

import android.content.Context;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.util.PluginUtil;
import org.opensilk.music.util.PriorityAsyncTask;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 10/5/14.
 */
@Singleton
public class NavLoader implements AsyncLoader<PluginInfo>, LoaderCallback<PluginInfo> {

    final Context context;
    final Set<LoaderCallback<PluginInfo>> callbacks;
    LoaderTask<PluginInfo> task;
    final List<PluginInfo> previousInfos;

    @Inject
    public NavLoader(@ForApplication Context context) {
        this.context = context;
        callbacks = new HashSet<>();
        previousInfos = new ArrayList<>();
    }

    @Override
    public void loadAsync(LoaderCallback<PluginInfo> callback) {
        //return cached copy
        if (!previousInfos.isEmpty()) {
            callback.onLoadComplete(previousInfos);
        }
        // prepare new query
        callbacks.add(callback);
        if (task == null) {
            task = new LoaderTask<PluginInfo>(context, this) {
                @Override
                protected List<PluginInfo> doInBackground(Void... params) {
                    List<PluginInfo> list = new ArrayList<>();
                    List<PluginInfo> extPlugins = PluginUtil.getActivePlugins(context);
                    if (extPlugins != null && !extPlugins.isEmpty()) {
                        list.addAll(extPlugins);
                    }
                    Collections.sort(list);
                    return list;
                }
            };
            task.execute();
        }
    }

    @Override
    public void onLoadComplete(List<PluginInfo> items) {
        previousInfos.clear();
        previousInfos.addAll(items);
        final Iterator<LoaderCallback<PluginInfo>> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            iterator.next().onLoadComplete(items);
            iterator.remove();
        }
        task = null;
    }

}
