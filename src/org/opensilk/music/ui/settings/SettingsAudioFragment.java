package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAudioFragment extends SettingsFragment implements
        Preference.OnPreferenceClickListener {

    private static final String PREF_EQUALIZER = "pref_equalizer";

    private Preference mEqualizer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_audio);
        mPrefSet = getPreferenceScreen();
        mEqualizer = mPrefSet.findPreference(PREF_EQUALIZER);
        mEqualizer.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mEqualizer) {
            if (MusicUtils.getAudioSessionId() == ERROR_BAD_VALUE) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.error)
                        .setMessage(R.string.no_audio_id)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            } else {
                NavUtils.openEffectsPanel(getActivity());
            }
            return true;
        }
        return false;
    }

}
