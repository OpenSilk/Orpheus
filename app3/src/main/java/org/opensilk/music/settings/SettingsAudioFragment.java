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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.R;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.ActivityResultCodes;

import javax.inject.Inject;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAudioFragment extends SettingsFragment implements
        Preference.OnPreferenceClickListener {

    private static final String PREF_EQUALIZER = "pref_equalizer";

    @Inject PlaybackController mMusicService;
    @Inject ActivityResultsController mActivytResultsController;

    private Preference mEqualizer;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SettingsActivityComponent component = DaggerService.getDaggerComponent(activity);
        component.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_audio);
        mPrefSet = getPreferenceScreen();

        mEqualizer = mPrefSet.findPreference(PREF_EQUALIZER);
        mEqualizer.setOnPreferenceClickListener(this);
        resolveEqualizer();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mEqualizer) {
            try {
                final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                effects.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getContext().getPackageName());
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                mActivytResultsController.startActivityForResult(effects, 0, null);
            } catch (final ActivityNotFoundException notFound) {
                Toast.makeText(getActivity(), getString(R.string.no_effects_for_you), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }

    /**
     * Restarts the app and service
     */
    private void doRestart() {
        // notify user of restart
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.settings_msg_restart_app)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Tells home activity is should initate a restart
                        mMusicService.playorPause();
                        mActivytResultsController.setResultAndFinish(ActivityResultCodes.RESULT_RESTART_FULL, null);
                    }
                })
                .show();
    }

    private void resolveEqualizer() {
        final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        PackageManager pm = getActivity().getPackageManager();
        ResolveInfo ri = pm.resolveActivity(effects, 0);
        if (ri == null) {
            mEqualizer.setEnabled(false);
            mEqualizer.setSummary(R.string.settings_equalizer_none);
        }
    }
}
