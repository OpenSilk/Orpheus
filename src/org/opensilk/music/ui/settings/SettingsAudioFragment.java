package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.TextUtils;

import org.opensilk.music.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.opensilk.cast.util.CastPreferences;
import org.opensilk.music.AppModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.silkdagger.DaggerInjector;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;
import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;
import static org.opensilk.music.ui2.event.ActivityResult.RESULT_RESTART_FULL;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAudioFragment extends SettingsFragment implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

    private static final String PREF_EQUALIZER = "pref_equalizer";
    private static final String PREF_DEFAULT_FOLDER = AppPreferences.PREF_DEFAULT_MEDIA_FOLDER;
    private static final String PREF_CASTING = CastPreferences.KEY_CAST_ENABLED;

    @Inject
    protected AppPreferences mSettings;
    private Preference mEqualizer;
    private CheckBoxPreference mCasting;
    private Preference mDefaultFolder;

    @dagger.Module (addsTo = AppModule.class, injects = SettingsAudioFragment.class)
    public static class Module {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity.getApplication()).getObjectGraph().plus(new Object[]{new Module()}).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_audio);
        mPrefSet = getPreferenceScreen();
        mEqualizer = mPrefSet.findPreference(PREF_EQUALIZER);
        mEqualizer.setOnPreferenceClickListener(this);
        resolveEqualizer();
        mCasting = (CheckBoxPreference) mPrefSet.findPreference(PREF_CASTING);
        mCasting.setChecked(CastPreferences.getBoolean(getActivity(), PREF_CASTING, true));
        mCasting.setOnPreferenceChangeListener(this);
        if (ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity())) {
            CastPreferences.putBoolean(getActivity(), PREF_CASTING, false);
            mCasting.setChecked(false);
            mCasting.setEnabled(false);
            mCasting.setSummary(R.string.settings_gms_unavailable);
        }
        mDefaultFolder = mPrefSet.findPreference(PREF_DEFAULT_FOLDER);
        String folder = mSettings.getString(PREF_DEFAULT_FOLDER, null);
        if (!TextUtils.isEmpty(folder)) {
            mDefaultFolder.setSummary(folder);
        }
        mDefaultFolder.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mEqualizer) {
            if (MusicUtils.getAudioSessionId() == ERROR_BAD_VALUE) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.error)
                        .setMessage(R.string.settings_err_no_audio_id)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            } else {
                NavUtils.openEffectsPanel(getActivity());
            }
            return true;
        } else if (preference == mDefaultFolder) {
            Intent i = new Intent(getActivity(), FolderPickerActivity.class);
            i.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME,
                    getActivity().getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false));
            String folder = mSettings.getString(PREF_DEFAULT_FOLDER, null);
            if (!TextUtils.isEmpty(folder)) {
                i.putExtra(FolderPickerActivity.EXTRA_DIR, folder);
            }
            startActivityForResult(i, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                final String folder = data.getStringExtra(FolderPickerActivity.EXTRA_DIR);
                if (!TextUtils.isEmpty(folder)) {
                    mDefaultFolder.setSummary(folder);
                    mSettings.putString(PREF_DEFAULT_FOLDER, folder);
                }
            } else  {
                mDefaultFolder.setSummary(getString(R.string.settings_storage_default_folder_summary));
                mSettings.remove(PREF_DEFAULT_FOLDER);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCasting) {
            CastPreferences.putBoolean(getActivity(), PREF_CASTING, (Boolean) newValue);
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
                .setMessage(R.string.settings_msg_restart_app)
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
