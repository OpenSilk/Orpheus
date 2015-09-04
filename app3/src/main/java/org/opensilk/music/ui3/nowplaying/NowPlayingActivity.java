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
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.common.ui.mortar.ToolbarOwnerDelegate;
import org.opensilk.common.ui.mortarfragment.MortarFragmentActivity;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui.theme.OrpheusTheme;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.MortarScope;

/**
 * Created by drew on 5/9/15.
 */
public class NowPlayingActivity extends MortarFragmentActivity {

    @Inject protected PlaybackController mPlaybackController;
//    @Inject ToolbarOwner mToolbarOwner;

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.sliding_panel) @Optional SlidingUpPanelLayout mSlidingPanel;

//    ToolbarOwnerDelegate<NowPlayingActivity> mActionBarDelegate;

    public static void startSelf(Context context, boolean startQueue) {
        Intent i = new Intent(context, NowPlayingActivity.class);
        i.putExtra("startqueue", startQueue);
        context.startActivity(i);
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                NowPlayingActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void onScopeCreated(MortarScope scope) {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(scope);
        AppPreferences settings = component.appPreferences();
        OrpheusTheme theme = settings.getTheme();
        setTheme(settings.isDarkTheme() ? theme.dark : theme.light);
    }

    @Override
    protected void performInjection() {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    public int getContainerViewId() {
        return 0;//unsupported
    }

    protected void setupView() {
        setContentView(R.layout.activity_nowplaying);
        ButterKnife.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupView();

        if (mSlidingPanel != null && getIntent().getBooleanExtra("startqueue", false)) {
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }

//        mToolbarOwner.setConfig(ActionBarConfig.builder()
//                .setTitle("")
//                .setUpButtonEnabled(true)
////                .setTransparentBackground(true)
//                .build());
//        mActionBarDelegate = new ToolbarOwnerDelegate<>(this, mToolbarOwner, mToolbar);
//        mActionBarDelegate.onCreate();

        mPlaybackController.connect();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mActionBarDelegate.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlaybackController.notifyForegroundStateChanged(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlaybackController.notifyForegroundStateChanged(false);
    }

    /*
     * Action bar owner
     */

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        return mActionBarDelegate.onCreateOptionsMenu(menu) || super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        return mActionBarDelegate.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
//    }

    @Override
    public void onBackPressed() {
        if (mSlidingPanel != null && mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }
}
