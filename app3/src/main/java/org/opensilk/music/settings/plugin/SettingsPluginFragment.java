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

package org.opensilk.music.settings.plugin;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.R;
import org.opensilk.music.settings.SettingsActivityComponent;

import mortar.MortarScope;

/**
 * Created by drew on 6/8/14.
 */
public class SettingsPluginFragment extends Fragment {

    private static final Uri SEARCH_URI;
    static {
        SEARCH_URI = Uri.parse("https://play.google.com/store/search?q=Orpheus%20Plugin&c=apps");
    }

    MortarScope mScope;
    String mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsActivityComponent activityComponent = DaggerService.getDaggerComponent(getActivity());
        mScope = MortarScope.getScope(getActivity()).buildChild()
                .withService(DaggerService.DAGGER_SERVICE, SettingsPluginComponent.FACTORY.call(activityComponent))
                .build(SettingsPluginFragment.class.getName());

        if (getArguments() != null) {
            mTitle = getString(getArguments().getInt("title"));
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTitle != null) {
            getActivity().setTitle(mTitle);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScope != null && !mScope.isDestroyed()) {
            mScope.destroy();
            mScope= null;
        }
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return LayoutInflater.from(mScope.createContext(getActivity()))
                .inflate(R.layout.settings_plugin_recycler, container, false);
    }
}
