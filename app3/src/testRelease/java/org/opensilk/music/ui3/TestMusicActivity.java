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

package org.opensilk.music.ui3;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import mortar.MortarScope;

/**
 * Created by drew on 11/16/15.
 */
public class TestMusicActivity extends MusicActivity {
    @Override
    protected void setupContentView() {
        setContentView(R.layout.blank_framelayout_toolbar);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
        setTheme(R.style.Theme_Dark);
    }

    @Override
    protected void performInjection() {
        TestMusicActivityComponent cmp = DaggerService.getDaggerComponent(this);
        cmp.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return R.id.main;
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appCmp = DaggerService.getDaggerComponent(getApplicationContext());
        TestMusicActivityComponent cmp = TestMusicActivityComponent.FACTORY.call(appCmp);
        builder.withService(DaggerService.DAGGER_SERVICE, cmp);
    }
}
