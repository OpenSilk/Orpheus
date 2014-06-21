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

package org.opensilk.music.loaders;

import android.content.Context;

import com.andrew.apollo.loaders.WrappedAsyncTaskLoader;

import org.opensilk.music.api.PluginInfo;
import org.opensilk.music.util.PluginUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 6/16/14.
 */
public class NavigationLoader extends WrappedAsyncTaskLoader<List<PluginInfo>> {

    public NavigationLoader(Context context) {
        super(context);
    }

    @Override
    public List<PluginInfo> loadInBackground() {
        List<PluginInfo> list = new ArrayList<>();
        List<PluginInfo> extPlugins = PluginUtil.getActivePlugins(getContext());
        if (extPlugins != null && !extPlugins.isEmpty()) {
            list.addAll(extPlugins);
        }
        Collections.sort(list);
        return list;
    }

}
