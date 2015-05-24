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

package org.opensilk.music.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.settings.themepicker.ThemePickerActivity;
import org.opensilk.music.theme.OrpheusTheme;

import javax.inject.Inject;

import static org.opensilk.music.ui3.common.ActivityResultCodes.RESULT_RESTART_APP;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsInterfaceFragment extends SettingsFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String PREF_THEME_PICKER = "theme_picker";
    private static final String PREF_DARK_THEME = AppPreferences.WANT_DARK_THEME;

    @Inject AppPreferences mSettings;
    @Inject ActivityResultsController mActivityResultsController;

    private CheckBoxPreference mDarkTheme;
    private Preference mThemePicker;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SettingsActivityComponent component = DaggerService.getDaggerComponent(activity);
        component.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_interface);
        mPrefSet = getPreferenceScreen();

        mDarkTheme = (CheckBoxPreference) mPrefSet.findPreference(PREF_DARK_THEME);
        mDarkTheme.setOnPreferenceChangeListener(this);

        mThemePicker = mPrefSet.findPreference(PREF_THEME_PICKER);
        mThemePicker.setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDarkTheme) {
            doRestart();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mThemePicker) {
            Intent intent = new Intent(getActivity(), ThemePickerActivity.class);
            startActivityForResult(intent, 11);
            return false;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 11) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    OrpheusTheme orpheusTheme =
                            OrpheusTheme.valueOf(data.getStringExtra(ThemePickerActivity.EXTRA_PICKED_THEME));
                    mSettings.putString(AppPreferences.ORPHEUS_THEME, orpheusTheme.toString());
                    doRestart();
                } catch (IllegalArgumentException e) {

                }
            }
        }
    }

    private void doRestart() {
        // notify user of restart
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.settings_msg_restart_app)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Tells home activity is should initate a restart
                        mActivityResultsController.setResultAndFinish(RESULT_RESTART_APP, null);
                    }
                })
                .show();
    }

}
