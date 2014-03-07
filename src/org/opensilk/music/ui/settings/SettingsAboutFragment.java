package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.webkit.WebView;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAboutFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    private static final String PREF_LICENSES   = "pref_licenses";
    private static final String PREF_AUTHOR     = "pref_author";
    private static final String PREF_VERSION    = "pref_version";
    private static final String PREF_THANKS     = "pref_thanks";

    private Preference mLicenses;
    private Preference mThanks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_about);
        mPrefSet = getPreferenceScreen();

        mLicenses = mPrefSet.findPreference(PREF_LICENSES);
        mLicenses.setOnPreferenceClickListener(this);

        Preference version = mPrefSet.findPreference(PREF_VERSION);
        version.setSummary(BuildConfig.VERSION_NAME);

        mThanks = mPrefSet.findPreference(PREF_THANKS);
        mThanks.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference == mLicenses) {
            final WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.settings_open_source_licenses)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            builder.show();
            return true;
        } else if (preference == mThanks) {
            new ThanksDialogFragment().show(getActivity().getFragmentManager(), "thanksdialog");
            return true;
        }

        return false;
    }
}
