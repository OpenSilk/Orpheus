package org.opensilk.music.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import hugo.weaving.DebugLog;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsPhoneActivity extends ActionBarActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper themeHelper = ThemeHelper.getInstance(this);
        setTheme(themeHelper.getTheme());
        setContentView(R.layout.settings_fragment_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_arrow_left_white);

        if (savedInstanceState == null) {
            //Load the main fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_content, new SettingsMainFragment())
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
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
}
