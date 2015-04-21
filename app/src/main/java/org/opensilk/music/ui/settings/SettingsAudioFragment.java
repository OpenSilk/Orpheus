package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.widget.Toast;

import org.opensilk.common.rx.RxUtils;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.opensilk.cast.util.CastPreferences;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.common.dagger.DaggerInjector;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.app.Activity.RESULT_OK;
import static android.media.audiofx.AudioEffect.ERROR_BAD_VALUE;
import static org.opensilk.music.ui2.event.ActivityResult.RESULT_RESTART_FULL;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsAudioFragment extends SettingsFragment implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

    @dagger.Module (addsTo = SettingsActivity.Module.class, injects = SettingsAudioFragment.class)
    public static class Module {
    }

    private static final String PREF_EQUALIZER = "pref_equalizer";
    private static final String PREF_DEFAULT_FOLDER = AppPreferences.AUTO_SHUFFLE_FOLDER;
    private static final String PREF_CASTING = CastPreferences.KEY_CAST_ENABLED;

    @Inject MusicServiceConnection mMusicService;

    private Preference mEqualizer;
    private CheckBoxPreference mCasting;
    private Preference mDefaultFolder;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity).getObjectGraph().plus(new Object[]{new Module()}).inject(this);
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
        String folder = AppPreferences.readAutoShuffleDirectory(getActivity());
        if (!TextUtils.isEmpty(folder)) {
            mDefaultFolder.setSummary(folder);
        }
        mDefaultFolder.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mEqualizer) {
            RxUtils.observeOnMain(mMusicService.getAudioSessionId())
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer sessionId) {
                            if (sessionId == ERROR_BAD_VALUE) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.error)
                                        .setMessage(R.string.settings_err_no_audio_id)
                                        .setNeutralButton(android.R.string.ok, null)
                                        .show();
                            } else {
                                try {
                                    final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                                    effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                                    effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                                    startActivityForResult(effects, 0);
                                } catch (final ActivityNotFoundException notFound) {
                                    Toast.makeText(getActivity(), getString(R.string.no_effects_for_you), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
            return true;
        } else if (preference == mDefaultFolder) {
            Intent i = new Intent(getActivity(), FolderPickerActivity.class);
            startActivityForResult(i, 11);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                final String folder = data.getStringExtra(FolderPickerActivity.EXTRA_DIR);
                if (!TextUtils.isEmpty(folder)) {
                    mDefaultFolder.setSummary(folder);
                    AppPreferences.writeAutoShuffleDirectory(getActivity(), folder);
                }
            } else {
                mDefaultFolder.setSummary(getString(R.string.settings_storage_default_folder_summary));
                AppPreferences.writeAutoShuffleDirectory(getActivity(), null);
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
                        if (mMusicService.isPlaying().toBlocking().first()) {
                            mMusicService.playOrPause();
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
