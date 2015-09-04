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

package org.opensilk.music.ui3.dragswipe;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui3.MusicActivity;
import org.opensilk.music.ui3.profile.ProfileScreen;

import mortar.MortarScope;

/**
 * Created by drew on 5/13/15.
 */
public class TracksDragSwipeActivity extends MusicActivity {

    public static void startSelf(Context context, ProfileScreen screen) {
        Intent i = new Intent(context, TracksDragSwipeActivity.class)
                .putExtra("screen", screen);
        context.startActivity(i);
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, TracksDragSwipeActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void performInjection() {
        TracksDragSwipeActivityComponent activityComponent = DaggerService.getDaggerComponent(this);
        activityComponent.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return R.id.main;
    }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_tracksdragswipe);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBarConfig config = ActionBarConfig.builder()
                .setUpButtonEnabled(true)
                .setTitle("")
                .setSubtitle("")
                .build();
//        mToolbarOwner.setConfig(config);

        ProfileScreen screen = getIntent().getParcelableExtra("screen");
        mFragmentManagerOwner.replaceMainContent(screen.getFragment(this), false);

    }
}
