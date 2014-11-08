package org.opensilk.music.ui.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.music.R;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui.activities.ActivityModule;
import org.opensilk.music.ui2.BaseActivity;
import org.opensilk.silkdagger.DaggerInjector;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((DaggerInjector) getApplication()).getObjectGraph().plus(getModules()).inject(this);

        setContentView(R.layout.blank_framelayout);

        boolean light = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        setTheme(light ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            //Load the main fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new SettingsMainFragment())
                    .commit();
        }

    }

    @Override
    public void onBackPressed() {
        // we use system fragment manager with PreferenceFragments
        if (getFragmentManager().popBackStackImmediate()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    //@DebugLog
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment f = getFragmentManager().findFragmentByTag(SettingsDonateFragment.class.getName());
        if (f != null) {
            f.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
        };
    }
}
