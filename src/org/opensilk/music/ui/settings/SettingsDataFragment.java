package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;

import org.apache.commons.io.FileUtils;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.artwork.cache.CacheUtil;

import java.util.Locale;

import static org.opensilk.music.artwork.ArtworkManager.DISK_CACHE_DIRECTORY;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsDataFragment extends SettingsFragment {

    private ListPreference mCacheSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_data);


        mCacheSize = (ListPreference) findPreference(PreferenceUtils.PREF_CACHE_SIZE);
        mCacheSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setCacheSizeSummary(Integer.valueOf((String) newValue));
                return true;
            }
        });

        setCacheSizeSummary(PreferenceUtils.getInstance(
                getActivity()).imageCacheSizeBytes() / 1024 / 1024);
        setupDeleteCache();
    }

    private void setCacheSizeSummary(int size) {
        if (mCacheSize != null) {
            mCacheSize.setSummary(String.format(Locale.US, "%.02f/%d MB",
                    (float) FileUtils.sizeOfDirectory(CacheUtil.getCacheDir(
                    getActivity(),DISK_CACHE_DIRECTORY)) / 1024 / 1024, size));
        }
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
                                    dialog.dismiss();
                                    if (ArtworkManager.clearCaches()) {
                                        Toast.makeText(getActivity(), "Caches cleared", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getActivity(), "Failed to clear caches", Toast.LENGTH_LONG).show();
                                    }
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
