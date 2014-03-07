package org.opensilk.music.ui.settings;

import android.os.Bundle;

import com.andrew.apollo.R;

/**
 * Created by andrew on 3/1/14.
 */
public class SettingsCastFragment extends SettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_cast);
    }

}
