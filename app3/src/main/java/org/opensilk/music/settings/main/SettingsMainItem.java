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

package org.opensilk.music.settings.main;

import android.os.Bundle;

import org.opensilk.music.R;
import org.opensilk.music.settings.SettingsAboutFragment;
import org.opensilk.music.settings.SettingsAudioFragment;
import org.opensilk.music.settings.SettingsDataFragment;
import org.opensilk.music.settings.SettingsInterfaceFragment;

/**
 * Created by drew on 5/18/15.
 */
public enum SettingsMainItem {

    UI(SettingsInterfaceFragment.class.getName(),
        R.string.settings_ui_category),
    DATA(SettingsDataFragment.class.getName(),
        R.string.settings_data_category),
    AUDIO(SettingsAudioFragment.class.getName(),
        R.string.settings_audio_category),
    HELP("help", //XXX hack
        R.string.settings_help_category),
    ABOUT(SettingsAboutFragment.class.getName(),
        R.string.settings_about_category);

    String className;
    int title;

    SettingsMainItem(String className, int title) {
        this.className = className;
        this.title = title;
    }

    Bundle getArguments() {
        Bundle b = new Bundle();
        b.putInt("title", title);
        return b;
    }
}
