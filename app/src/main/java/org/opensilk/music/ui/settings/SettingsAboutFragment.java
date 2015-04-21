package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

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
            createOpenSourceDialog().show();
            return true;
        } else if (preference == mVersion) {
            createChangesDialog().show();
            return true;
        } else if (preference == mThanks) {
            new ThanksDialogFragment().show(getActivity().getFragmentManager(), "thanksdialog");
            return true;
        }
        return false;
    }

    AlertDialog createOpenSourceDialog() {
        final WebView webView = new WebView(getActivity());
        webView.loadUrl("file:///android_asset/licenses.html");
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings_open_source_licenses)
                .setView(webView)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    AlertDialog createChangesDialog() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.changes_dialog, null);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings_changes_dialog_title)
                .setView(v)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
