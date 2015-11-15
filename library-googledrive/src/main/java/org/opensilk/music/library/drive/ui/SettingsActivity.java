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

package org.opensilk.music.library.drive.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.drive.R;

/**
 * Created by drew on 11/15/15.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean light = getIntent().getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, true);
        setTheme(light ? R.style.DriveLightTheme : R.style.DriveDarkTheme);
        super.onCreate(savedInstanceState);
        FrameLayout main = new FrameLayout(this);
        main.setId(R.id.main);
        setContentView(main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new SettingsFragment(), "settings_fragment")
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            Preference version = findPreference("version");
            version.setSummary(getVersion(getActivity()));
            Preference licenses = findPreference("licenses");
            licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    createOpenSourceDialog().show();
                    return true;
                }
            });
        }

        private static String getVersion(Context context) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                return info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                //better never happen we're calling ourselves
                throw new RuntimeException(e);
            }
        }

        AlertDialog createOpenSourceDialog() {
            final WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.drive_open_source_licenses)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }
}
