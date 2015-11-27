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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.opensilk.music.R;
import org.opensilk.music.artwork.cache.CacheUtil;
import org.opensilk.music.artwork.shared.ArtworkPreferences;

import java.util.Locale;

import static org.opensilk.music.artwork.Constants.DISK_CACHE_DIRECTORY;
import static org.opensilk.music.artwork.fetcher.ArtworkFetcherService.clearCache;
import static org.opensilk.music.artwork.fetcher.ArtworkFetcherService.reloadPrefs;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsDataFragment extends SettingsFragment {

    ListPreference mCacheSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(ArtworkPreferences.NAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);

        addPreferencesFromResource(R.xml.settings_data);

        mCacheSize = (ListPreference) findPreference(ArtworkPreferences.IMAGE_DISK_CACHE_SIZE);
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

    @Override
    public void onPause() {
        super.onPause();
        reloadPrefs(getActivity());
    }

    int getCacheSize() {
        return Integer.decode(getPreferenceManager().getSharedPreferences()
                .getString(ArtworkPreferences.IMAGE_DISK_CACHE_SIZE, ArtworkPreferences.IMAGE_DISK_CACHE_DEFAULT));
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
                    final Context context = getActivity();
                    new AlertDialog.Builder(context).setMessage(R.string.settings_delete_warning)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which) {
                                    dialog.dismiss();
                                    clearCache(context);
                                    Toast.makeText(context, R.string.settings_msg_cache_cleared, Toast.LENGTH_LONG).show();
                                    setCacheSizeSummary(getCacheSize());
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
