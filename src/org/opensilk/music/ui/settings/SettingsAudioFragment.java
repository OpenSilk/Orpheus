package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;
import static org.opensilk.music.ui.activities.HomeSlidingActivity.RESULT_RESTART_APP;
import static org.opensilk.music.ui.activities.HomeSlidingActivity.RESULT_RESTART_FULL;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAudioFragment extends SettingsFragment implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

    private static final String PREF_EQUALIZER = "pref_equalizer";

    private PreferenceUtils mPreferences;
    private Preference mEqualizer;
    private CheckBoxPreference mCasting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_audio);
        mPreferences = PreferenceUtils.getInstance(getActivity());
        mPrefSet = getPreferenceScreen();
        mEqualizer = mPrefSet.findPreference(PREF_EQUALIZER);
        mEqualizer.setOnPreferenceClickListener(this);
        mCasting = (CheckBoxPreference) mPrefSet.findPreference(PreferenceUtils.KEY_CAST_ENABLED);
        mCasting.setChecked(mPreferences.isCastEnabled());
        mCasting.setOnPreferenceChangeListener(this);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCasting) {
            mPreferences.setCastEnabled((Boolean) newValue);
            mCasting.setChecked((Boolean) newValue);
            doRestart();
            return false;
        }
        return false;
    }

    /**
     * Restarts the app and service
     */
    private void doRestart() {
        // notify user of restart
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.settings_interface_restart_app)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Tells home activity is should initate a restart
                        if (MusicUtils.isPlaying()) {
                            MusicUtils.playOrPause();
                        }
                        getActivity().setResult(RESULT_RESTART_FULL);
                        getActivity().finish();
                    }
                })
                .show();
    }
}
