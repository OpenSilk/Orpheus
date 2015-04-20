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

package org.opensilk.music.ui2.library;

import android.os.Parcel;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.PluginInfo;

import flow.Layout;

/**
 * Created by drew on 10/6/14.
 */
@Layout(R.layout.library_plugin)
@WithModule(PluginScreenModule.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.shrink_fade_out, R.anim.slide_in_left }
)
public class PluginScreen extends Screen {

    final PluginInfo pluginInfo;

    public PluginScreen(PluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }

    @Override
    public String getName() {
        return super.getName() + pluginInfo.componentName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(pluginInfo, flags);
        super.writeToParcel(dest, flags);
    }

    public static final Creator<PluginScreen> CREATOR = new Creator<PluginScreen>() {
        @Override
        public PluginScreen createFromParcel(Parcel source) {
            PluginScreen s = new PluginScreen(
                    source.<PluginInfo>readParcelable(PluginInfo.class.getClassLoader())
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public PluginScreen[] newArray(int size) {
            return new PluginScreen[size];
        }
    };

}
