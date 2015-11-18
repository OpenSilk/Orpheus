/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.settings;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.settings.main.SettingsMainFragment;
import org.opensilk.music.ui3.MusicActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;

/**
 * Created by andrew on 2/28/14.
 */
public class SettingsActivity extends MusicActivity {

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;

    @Override
    protected void setupContentView() {
        setContentView(R.layout.blank_framelayout_toolbar);
        ButterKnife.inject(this);
    }

    @Override
    protected void performInjection() {
        SettingsActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;//unsupported, using preference fragments
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent component = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, SettingsActivityComponent.FACTORY.call(component));
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
        boolean lightTheme = !preferences.isDarkTheme();
        setTheme(lightTheme ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolbarOwner.attachToolbar(mToolbar);

        if (savedInstanceState == null) {
            //Load the main fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new SettingsMainFragment())
                    .commit();
        }

    }

    @Override
    protected void onDestroy() {
        mToolbarOwner.detachToolbar(mToolbar);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;//dont want mediarouter
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
