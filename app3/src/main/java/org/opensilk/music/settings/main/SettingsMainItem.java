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
import org.opensilk.music.settings.plugin.SettingsPluginFragment;

/**
 * Created by drew on 5/18/15.
 */
public enum SettingsMainItem {

    UI(SettingsInterfaceFragment.class.getName(),
        R.string.settings_ui_category,
        R.drawable.ic_phone_android_grey600_36dp),
    DATA(SettingsDataFragment.class.getName(),
        R.string.settings_data_category,
        R.drawable.ic_data_usage_grey600_36dp),
    AUDIO(SettingsAudioFragment.class.getName(),
        R.string.settings_audio_category,
        R.drawable.ic_tune_grey600_36dp),
    PLUGIN(SettingsPluginFragment.class.getName(),
        R.string.settings_plugin_category,
        R.drawable.ic_extension_grey600_36dp),
    // XXX add new items above this one.
    DONATE("donate", //XXX hack
        R.string.settings_donate_category,
        R.drawable.ic_attach_money_grey600_36dp),
    ABOUT(SettingsAboutFragment.class.getName(),
        R.string.settings_about_category,
        R.drawable.ic_info_outline_grey600_36dp);

    String className;
    int title;
    int iconRes;

    SettingsMainItem(String className, int title, int iconRes) {
        this.className = className;
        this.title = title;
        this.iconRes = iconRes;
    }

    Bundle getArguments() {
        Bundle b = new Bundle();
        b.putInt("title", title);
        b.putInt("icon", iconRes);
        return b;
    }
}
