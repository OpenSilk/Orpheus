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

package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.loader.PluginLoader;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by drew on 6/8/14.
 */
public class SettingsPluginFragment extends SettingsFragment implements Preference.OnPreferenceChangeListener {

    @dagger.Module(
            addsTo = AppModule.class,
            injects = SettingsPluginFragment.class
    )
    public static class Module {

    }

    private static final Uri SEARCH_URI;
    static {
        SEARCH_URI = Uri.parse("https://play.google.com/store/search?q=Orpheus%20Plugin&c=apps");
    }

    @Inject PluginLoader loader;
    List<PluginInfo> pluginInfos;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity.getApplication()).getObjectGraph().plus(new Module()).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.blank_prefscreen);
        // Pull list of available dreams
        pluginInfos = loader.getPluginInfos(true);
        // Add all available dreams to preference screen
        // yea its hackish but much less typing compared to a list view
        for (PluginInfo info : pluginInfos) {
            PluginPreference p = new PluginPreference(getActivity(), info);
            p.setOnPreferenceChangeListener(this);
            getPreferenceScreen().addPreference(p);
        }
        addFindPluginPref();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isChecked = ((Boolean) newValue);
        for (int ii=0; ii<getPreferenceScreen().getPreferenceCount(); ii++) {
            Preference pref = getPreferenceScreen().getPreference(ii);
            if (pref instanceof PluginPreference) {
                PluginPreference pluginPref = (PluginPreference) pref;
                if (preference == pluginPref) {
                    // Mark as active
                    pluginPref.pluginInfo.isActive = isChecked;
                    // Store the new dream component
                    if (isChecked) {
                        loader.setPluginEnabled(pluginPref.pluginInfo.componentName);
                    } else {
                        loader.setPluginDisabled(pluginPref.pluginInfo.componentName);
                    }
                }
            }
        }
        return true;
    }

    private void addFindPluginPref() {
        Preference p = new Preference(getActivity());
        p.setTitle(getString(R.string.settings_get_plugins));
        p.setSummary(getString(R.string.settings_get_plugins_summary));
        p.setIntent(new Intent(Intent.ACTION_VIEW).setData(SEARCH_URI));
        getPreferenceScreen().addPreference(p);
    }

    public static class PluginPreference extends CheckBoxPreference {
        PluginInfo pluginInfo;
        public PluginPreference(Context context, PluginInfo info) {
            super(context);
            pluginInfo = info;
            setTitle(info.title);
            setSummary(info.description);
            setIcon(info.icon);
            setChecked(info.isActive);
        }
    }
}
