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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui3.MusicActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.MortarScope;

/**
 * Created by drew on 10/2/15.
 */
public class NowPlayingActivity extends MusicActivity {

    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;

    public static void startSelf(Context context, boolean startQueue) {
        Intent i = new Intent(context, NowPlayingActivity.class);
        i.putExtra("startqueue", startQueue);
        context.startActivity(i);
    }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_nowplaying);
        ButterKnife.inject(this);
//        mDrawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {
//        OrpheusTheme theme = preferences.getTheme();
//        setTheme(preferences.isDarkTheme() ? theme.dark : theme.light);
    }

    @Override
    protected void performInjection() {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                NowPlayingActivityComponent.FACTORY.call(appComponent));
    }
}
