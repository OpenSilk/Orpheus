package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.theme.OrpheusTheme;

import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.andrew.apollo.utils.ThemeStyle;

import java.util.Locale;

import javax.inject.Inject;

import static org.opensilk.music.ui2.event.ActivityResult.RESULT_RESTART_APP;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsInterfaceFragment extends SettingsFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @dagger.Module(addsTo = SettingsActivity.Module.class, injects = SettingsInterfaceFragment.class)
    public static class Module {
    }

    private static final String PREF_THEME_PICKER = "theme_picker";
    private static final String PREF_DARK_THEME = AppPreferences.WANT_DARK_THEME;
    private static final String PREF_HOME_PAGES = AppPreferences.HOME_PAGES;

    @Inject AppPreferences mSettings;

    private CheckBoxPreference mDarkTheme;
    private Preference mThemePicker;
    private DragSortSwipeListPreference mHomePages;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity).getObjectGraph().plus(new Object[]{new Module()}).inject(this);
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

        mHomePages = (DragSortSwipeListPreference) mPrefSet.findPreference(PREF_HOME_PAGES);
        mHomePages.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDarkTheme) {
            doRestart();
            return true;
        } else if (preference == mHomePages) {
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
                        getActivity().setResult(RESULT_RESTART_APP);
                        getActivity().finish();
                    }
                })
                .show();
    }

}
