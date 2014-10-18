package org.opensilk.music.ui.settings;

import android.os.Bundle;
import android.preference.Preference;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;
import com.andrew.apollo.utils.ApolloUtils;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAboutFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    private static final String PREF_LICENSES   = "pref_licenses";
    private static final String PREF_AUTHOR     = "pref_author";
    private static final String PREF_VERSION    = "pref_version";
    private static final String PREF_THANKS     = "pref_thanks";

    private Preference mLicenses;
    private Preference mVersion;
    private Preference mThanks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_about);
        mPrefSet = getPreferenceScreen();

        mLicenses = mPrefSet.findPreference(PREF_LICENSES);
        mLicenses.setOnPreferenceClickListener(this);

        mVersion = mPrefSet.findPreference(PREF_VERSION);
        mVersion.setSummary(BuildConfig.VERSION_NAME);
        mVersion.setOnPreferenceClickListener(this);


        mThanks = mPrefSet.findPreference(PREF_THANKS);
        mThanks.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference == mLicenses) {
            ApolloUtils.createOpenSourceDialog(getActivity()).show();
            return true;
        } else if (preference == mVersion) {
            ApolloUtils.createChangesDialog(getActivity()).show();
            return true;
        } else if (preference == mThanks) {
            new ThanksDialogFragment().show(getActivity().getFragmentManager(), "thanksdialog");
            return true;
        }

        return false;
    }
}
