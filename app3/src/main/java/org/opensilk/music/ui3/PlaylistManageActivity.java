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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.mortar.DialogFactory;
import org.opensilk.common.ui.mortar.DialogFactoryFragment;
import org.opensilk.common.ui.mortar.DialogPresenter;
import org.opensilk.common.ui.mortar.DialogPresenterActivity;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui3.playlist.PlaylistChooseScreenFragment;

import java.util.List;

import javax.inject.Inject;

import mortar.MortarScope;

/**
 * Created by drew on 10/23/15.
 */
public class PlaylistManageActivity extends MusicActivity implements DialogPresenterActivity {

    public static void startSelf(Context context) {
        startSelf(context, null);
    }

    public static void startSelf(Context context, List<Uri> tracksUris) {
        context.startActivity(makeIntent(context, tracksUris));
    }

    public static Intent makeIntent(Context context, List<Uri> tracksUris) {
        Intent i = new Intent(context, PlaylistManageActivity.class);
        i.putExtra("b", BundleHelper.b().putList(tracksUris).get());
        return i;
    }

    @Override
    protected void setupContentView() {
        setContentView(R.layout.activity_playlist);
    }

    @Override
    protected void themeActivity(AppPreferences preferences) {

    }

    @Override
    protected void performInjection() {
        PlaylistManageActivityComponent cmp = DaggerService.getDaggerComponent(this);
        cmp.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return R.id.main;
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        PlaylistManageActivityComponent cmp = PlaylistManageActivityComponent.FACTORY.call(parent);
        builder.withService(DaggerService.DAGGER_SERVICE, cmp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        if (savedInstanceState == null) {
            Bundle args = getIntent().getBundleExtra("b");
            mFragmentManagerOwner.replaceMainContent(
                    PlaylistChooseScreenFragment.ni(this, args), false);
        }
    }

    /*
     * Toolbar
     */

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected boolean hasLeftDrawer() {
        return false;
    }


}
