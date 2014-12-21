package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import org.apache.commons.io.FileUtils;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.cache.CacheUtil;
import org.opensilk.common.dagger.DaggerInjector;

import java.util.Locale;

import javax.inject.Inject;

import static org.opensilk.music.artwork.ArtworkModule.DISK_CACHE_DIRECTORY;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsDataFragment extends SettingsFragment {

    @dagger.Module(addsTo = SettingsActivity.Module.class, injects = SettingsDataFragment.class)
    public static class Module {

    }

    @Inject AppPreferences mSettings;
    @Inject ArtworkRequestManager mRequestor;

    ListPreference mCacheSize;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity).getObjectGraph().plus(new Module()).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_data);

        mCacheSize = (ListPreference) findPreference(AppPreferences.IMAGE_DISK_CACHE_SIZE);
        mCacheSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setCacheSizeSummary(Integer.decode((String) newValue));
                return true;
            }
        });

        setCacheSizeSummary(getCacheSize());
        setupDeleteCache();
    }

    int getCacheSize() {
        return Integer.decode(mSettings.getString(AppPreferences.IMAGE_DISK_CACHE_SIZE, "60"));
    }

    void setCacheSizeSummary(int size) {
        if (mCacheSize != null) {
            mCacheSize.setSummary(String.format(Locale.US, "%.02f/%d MB",
                    (float) FileUtils.sizeOfDirectory(CacheUtil.getCacheDir(
                    getActivity(),DISK_CACHE_DIRECTORY)) / 1024 / 1024, size));
        }
    }

    /**
     * Removes all of the cache entries.
     */
    void setupDeleteCache() {
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
                                    if (mRequestor.clearCaches()) {
                                        Toast.makeText(getActivity(), "Caches cleared", Toast.LENGTH_LONG).show();
                                        setCacheSizeSummary(getCacheSize());
                                    } else {
                                        Toast.makeText(getActivity(), "Failed to clear caches", Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    return true;
                }
            });
        }
    }

}
