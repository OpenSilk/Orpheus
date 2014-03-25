package org.opensilk.music.ui.settings;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.andrew.apollo.R;

import org.opensilk.music.util.ConfigHelper;

import java.util.List;

/**
 * Created by andrew on 3/22/14.
 */
public class SettingsTabletActivity extends PreferenceActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
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
    protected boolean isValidFragment (String fragmentName) {
        if (SettingsDataFragment.class.getName().equals(fragmentName)) {
            return true;
        } else if (SettingsInterfaceFragment.class.getName().equals(fragmentName)) {
            return true;
        } else if (SettingsAudioFragment.class.getName().equals(fragmentName)) {
            return true;
        } else if (SettingsAboutFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        Resources res = getResources();
        if (ConfigHelper.isPortrait(res) && !ConfigHelper.isXLargeScreen(res)) return false;
        return true;
    }
}
