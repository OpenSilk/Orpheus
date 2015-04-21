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
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;

import flow.Layout;

/**
 * Created by drew on 10/5/14.
 */
@Layout(R.layout.library)
@WithModule(LibraryScreenModule.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left },
        replace = { R.anim.slide_out_right, R.anim.grow_fade_in }
)
public class LibraryScreen extends Screen {

    final PluginInfo pluginInfo;
    final PluginConfig pluginConfig;
    final LibraryInfo libraryInfo;

    public LibraryScreen(PluginInfo pluginInfo, PluginConfig pluginConfig, LibraryInfo libraryInfo) {
        this.pluginInfo = pluginInfo;
        this.pluginConfig = pluginConfig;
        this.libraryInfo = libraryInfo;
    }

    @Override
    public String getName() {
        return super.getName() + libraryInfo.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(pluginInfo, flags);
        dest.writeBundle(pluginConfig.dematerialize());
        dest.writeParcelable(libraryInfo, flags);
        super.writeToParcel(dest, flags);
    }

    public static final Creator<LibraryScreen> CREATOR = new Creator<LibraryScreen>() {
        @Override
        public LibraryScreen createFromParcel(Parcel source) {
            LibraryScreen s = new LibraryScreen(
                    source.<PluginInfo>readParcelable(PluginInfo.class.getClassLoader()),
                    PluginConfig.materialize(source.readBundle()),
                    source.<LibraryInfo>readParcelable(LibraryInfo.class.getClassLoader())
            );
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public LibraryScreen[] newArray(int size) {
            return new LibraryScreen[size];
        }
    };

}
