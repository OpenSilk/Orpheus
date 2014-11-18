/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.nowplaying.NowPlayingScreen;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Layouts;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingActivity extends BaseMortarActivity {

    public static class Blueprint extends BaseMortarActivity.Blueprint {
        public Blueprint(String scopeName) {
            super(scopeName);
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }
    }

    @dagger.Module(
            includes = BaseMortarActivity.Module.class,
            injects = NowPlayingActivity.class
    )
    public static class Module {
    }

    @InjectView(R.id.now_playing_toolbar) Toolbar toolbar;

    @Override
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        OrpheusTheme orpheusTheme = mSettings.getTheme();
        setTheme(mSettings.isDarkTheme() ? orpheusTheme.profileDark : orpheusTheme.profileLight);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MortarContextFactory contextFactory = new MortarContextFactory();
        setContentView(Layouts.createView(contextFactory.setUpContext(new NowPlayingScreen(), this), NowPlayingScreen.class));
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }



}
