package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.opensilk.music.R;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.andrew.apollo.utils.ThemeStyle;

import java.util.Locale;

import static org.opensilk.music.ui2.event.ActivityResult.RESULT_RESTART_APP;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsInterfaceFragment extends SettingsFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_THEME = "pref_theme";
    private static final String PREF_DARK_THEME = "pref_dark_theme";
    private static final String PREF_HOME_PAGES = "pref_home_pages";

    private CheckBoxPreference mDarkTheme;
    private ThemeListPreference mThemeList;
    private DragSortSwipeListPreference mHomePages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_interface);
        mPrefSet = getPreferenceScreen();

        mDarkTheme = (CheckBoxPreference) mPrefSet.findPreference(PREF_DARK_THEME);
        mDarkTheme.setOnPreferenceChangeListener(this);

        mThemeList = (ThemeListPreference) mPrefSet.findPreference(PREF_THEME);
        mThemeList.setOnPreferenceChangeListener(this);
        updateThemeIcon(ThemeHelper.getInstance(getActivity()).getThemeName());

        mHomePages = (DragSortSwipeListPreference) mPrefSet.findPreference(PREF_HOME_PAGES);
        mHomePages.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThemeList) {
            new ThemePickerDialogFragment().show(getFragmentManager(), null);
//            String newTheme = (String) newValue;
//            String currentTheme = ThemeHelper.getInstance(getActivity()).getThemeName();
//            if (PreferenceUtils.getInstance(getActivity()).wantDarkTheme()) {
//                newTheme += "DARK"; //Chooser only has reg themes, so append dark
//            }
//            if (!newTheme.equalsIgnoreCase(currentTheme)) {
//                updateThemeIcon(newTheme);
//                applyTheme(ThemeStyle.valueOf(newTheme.toUpperCase(Locale.US)));
//                doRestart();
//            }
            return false; // We set preference
        } else if (preference == mDarkTheme) {
            String currentTheme = ThemeHelper.getInstance(getActivity()).getThemeName().toUpperCase(Locale.US);
            ThemeStyle newTheme;
            if ((Boolean) newValue) {
                if (currentTheme.contains("DARK")) {
                    //Already on dark theme use it
                    newTheme = ThemeStyle.valueOf(currentTheme);
                } else {
                    //Convert to equivalent dark theme
                    newTheme = ThemeStyle.valueOf(currentTheme + "DARK");
                }
            } else {
                if (currentTheme.contains("DARK")) {
                    //Convert ot equivalent light theme
                    newTheme = ThemeStyle.valueOf(currentTheme.replace("DARK", ""));
                } else {
                    // Already on light theme use it
                    newTheme = ThemeStyle.valueOf(currentTheme);
                }
            }
            applyTheme(newTheme);
            doRestart();
            return true;
        } else if (preference == mHomePages) {
            doRestart();
            return true;
        }
        return false;
    }

    private void applyTheme(ThemeStyle newTheme) {
        // Update prefrence
        PreferenceUtils.getInstance(getActivity()).setThemeStyle(newTheme);
        // refresh the singleton
        ThemeHelper.getInstance(getActivity()).reloadTheme();
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
                        getActivity().setResult(RESULT_RESTART_APP);
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void updateThemeIcon(String name) {
        mThemeList.setIcon(new ColorDrawable(ThemeHelper.getInstance(getActivity())
                .getThemePrimaryColor(ThemeStyle.valueOf(name.toUpperCase(Locale.US)))));
    }

}
