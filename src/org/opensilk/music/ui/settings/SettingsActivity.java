package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.WindowManager;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsActivity extends ActionBarActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper themeHelper = ThemeHelper.getInstance(this);
        setTheme(themeHelper.getDialogTheme());

        setupFauxDialog();

        setContentView(R.layout.settings_fragment_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        //hacks cant set null FIXME real drawable so we can get some padding
        actionBar.setHomeAsUpIndicator(R.drawable.blank);
        actionBar.setIcon(ThemeHelper.isDialog(this) ? R.drawable.ic_action_cancel_white : R.drawable.ic_action_arrow_left_white);

        if (savedInstanceState == null) {
            //Load the main fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_content, new SettingsMainFragment())
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

    // Thanks dashclock for this
    private void setupFauxDialog() {
        // Check if this should be a dialog
        if (!ThemeHelper.isDialog(this)) {
            return;
        }

        // Should be a dialog; set up the window parameters.
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.profile_dialog_width);
        params.height = Math.min(
                getResources().getDimensionPixelSize(R.dimen.profile_dialog_max_height),
                dm.heightPixels * 7 / 8);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
}
