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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.andrew.apollo.menu.AddToPlaylistDialog;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.utils.MusicUtils;
import com.google.gson.Gson;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.GsonParcer;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.FrameScreenSwitcherView;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.core.android.ActionBarOwner.CustomMenuItem;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenDialog;
import org.opensilk.music.ui2.loader.LoaderModule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import flow.Flow;
import flow.Parcer;
import mortar.Blueprint;

/**
 * Created by drew on 11/7/14.
 */
public class BaseSwitcherActivity extends BaseMortarActivity implements
        AppFlowPresenter.Activity {

    @dagger.Module(
            includes = {
                    BaseMortarActivity.Module.class,
                    ActionBarOwner.Module.class, //Some classes need this even if there isnt an actionbar
                    LoaderModule.class,
            }, library = true
    )
    public static class Module {
        @Provides @Singleton
        public Parcer<Object> provideParcer(Gson gson) {
            return new GsonParcer<>(gson);
        }
        @Provides @Singleton
        public AppFlowPresenter<BaseSwitcherActivity> providePresenter(Parcer<Object> floParcer) {
            return new AppFlowPresenter<>(floParcer);
        }
    }

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
        mBus.register(this);
        mAppFlowPresenter.takeView(this);
        setupView();
        ButterKnife.inject(this);
    }

    @Override
    protected void onDestroy() {
        if (mBus != null) mBus.unregister(this);
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

    public void onEventMainThread(MakeToast e) {
        if (e.type == MakeToast.Type.PLURALS) {
            Toast.makeText(this, MusicUtils.makeLabel(this, e.resId, e.arg), Toast.LENGTH_SHORT).show();
        } else if (e.params.length > 0) {
            Toast.makeText(this, getString(e.resId, e.params), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, e.resId, Toast.LENGTH_SHORT).show();
        }
    }

    //TODO stop using fragments
    public void onEventMainThread(OpenDialog e) {
        e.dialog.show(getSupportFragmentManager(), "Dialog");
    }

    /*
     * AppFlowPresenter.Activity
     */

    @Override
    public void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback) {
        mContainer.showScreen(screen, direction, callback);
    }

}
