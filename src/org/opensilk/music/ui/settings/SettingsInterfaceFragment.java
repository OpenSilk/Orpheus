package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.andrew.apollo.utils.ThemeStyle;

import java.util.Locale;

import static org.opensilk.music.ui.activities.HomeSlidingActivity.RESULT_RESTART_APP;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsInterfaceFragment extends SettingsFragment implements Preference.OnPreferenceChangeListener {

    private static final String PREF_THEME = "pref_theme";

    private ThemeListPreference mThemeList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_interface);
        mPrefSet = getPreferenceScreen();
        mThemeList = (ThemeListPreference) mPrefSet.findPreference(PREF_THEME);
        mThemeList.setOnPreferenceChangeListener(this);
        updateThemIcon(ThemeHelper.getInstance(getActivity()).getThemeName());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThemeList) {
            String newTheme = (String) newValue;
            String currentTheme = ThemeHelper.getInstance(getActivity()).getThemeName();
            if (!newTheme.equalsIgnoreCase(currentTheme)) {
                doRestart(ThemeStyle.valueOf(newTheme.toUpperCase(Locale.US)));
            }
            return true;
        }
        return false;
    }

    private void doRestart(ThemeStyle newTheme) {
        // Update prefrence
        PreferenceUtils.getInstance(getActivity()).setThemeStyle(newTheme);
        // refresh the singleton
        ThemeHelper.getInstance(getActivity()).reloadTheme();
        // notify user of restart
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.settings_interface_restart_app)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Tells home activity is should initate a restart
                        getActivity().setResult(RESULT_RESTART_APP);
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void updateThemIcon(String name) {
        mThemeList.setIcon(new ColorDrawable(ThemeHelper.getInstance(getActivity())
                .getThemeColor(ThemeStyle.valueOf(name.toUpperCase(Locale.US)))));
    }
}
