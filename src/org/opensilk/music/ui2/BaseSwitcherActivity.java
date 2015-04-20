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
import android.view.MenuItem;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.FrameScreenSwitcherView;
import org.opensilk.music.R;
import org.opensilk.music.ui2.event.GoToScreen;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Flow;

/**
 * Created by drew on 11/7/14.
 */
public class BaseSwitcherActivity extends BaseMortarActivity implements
        AppFlowPresenter.Activity {

    @Inject AppFlowPresenter<BaseSwitcherActivity> mAppFlowPresenter;

    @InjectView(R.id.main) protected FrameScreenSwitcherView mContainer;

    @Override
    public Screen getDefaultScreen() {
        throw new UnsupportedOperationException("Subclass must override getDefaultScreen()");
    }

    protected void setupView() {
        throw new UnsupportedOperationException("Subclass must override setupView()");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppFlowPresenter.takeView(this);
        setupView();
        ButterKnife.inject(this);
    }

    @Override
    protected void onDestroy() {
        if (mAppFlowPresenter != null) mAppFlowPresenter.dropView(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mContainer.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return mContainer.onUpPressed() || super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (AppFlow.isAppFlowSystemService(name)) {
            return mAppFlowPresenter.getAppFlow();
        }
        return super.getSystemService(name);
    }

    /*
     * Events
     */

    public void onEventMainThread(GoToScreen e) {
        AppFlow.get(this).goTo(e.screen);
    }

    /*
     * AppFlowPresenter.Activity
     */

    @Override
    public void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback) {
        mContainer.showScreen(screen, direction, callback);
    }

}
