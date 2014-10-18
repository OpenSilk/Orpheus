package org.opensilk.music.ui.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.ui.activities.ActivityModule;
import org.opensilk.music.ui.activities.BaseDialogActivity;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsActivity extends BaseDialogActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
        };
    }
}
