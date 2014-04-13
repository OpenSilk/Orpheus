package org.opensilk.music.ui.settings;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.MenuItem;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.util.ConfigHelper;

import java.util.List;

/**
 * Created by andrew on 3/22/14.
 */
public class SettingsTabletActivity extends PreferenceActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //Needs to come before the super
        ThemeHelper themeHelper = ThemeHelper.getInstance(this);
        setTheme(themeHelper.getTheme());
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
        boolean light = ThemeHelper.isLightTheme(this);
        for (Header header : target) {
            if (SettingsDataFragment.class.getName().equals(header.fragment)) {
                header.iconRes = light ? R.drawable.ic_settings_data_light :
                        R.drawable.ic_settings_data_dark;
            } else if (SettingsInterfaceFragment.class.getName().equals(header.fragment)) {
                header.iconRes = light ? R.drawable.ic_settings_interface_light :
                        R.drawable.ic_settings_interface_dark;
            } else if (SettingsAudioFragment.class.getName().equals(header.fragment)) {
                header.iconRes = light ? R.drawable.ic_settings_audio_light :
                        R.drawable.ic_settings_audio_dark;
            } else if (SettingsAboutFragment.class.getName().equals(header.fragment)) {
                header.iconRes = light ? R.drawable.ic_settings_about_light :
                        R.drawable.ic_settings_about_dark;
            }

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
        return true;
    }
}
