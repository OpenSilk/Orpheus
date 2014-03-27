package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsDataFragment extends SettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_data);
        setupDeleteCache();
    }

    /**
     * Removes all of the cache entries.
     */
    private void setupDeleteCache() {
        final Preference deleteCache = findPreference("pref_delete_cache");
        if (deleteCache != null) {
            deleteCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    new AlertDialog.Builder(getActivity()).setMessage(R.string.delete_warning)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which) {
//                                    ApolloUtils.getImageFetcher(getActivity()).clearCaches();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    return true;
                }
            });
        }
    }

}
