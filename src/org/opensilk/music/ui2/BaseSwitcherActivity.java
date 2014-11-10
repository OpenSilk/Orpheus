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
import org.opensilk.music.ui2.event.ConfirmDelete;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.ui2.event.OpenAddToPlaylist;
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
        AppFlowPresenter.Activity,
        ActionBarOwner.Activity {

    @dagger.Module(
            includes = {
                    BaseMortarActivity.Module.class,
                    ActionBarOwner.Module.class,
                    LoaderModule.class,
            }, library = true
    )
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return new EventBus();
        }
        @Provides @Singleton
        public Parcer<Object> provideParcer(Gson gson) {
            return new GsonParcer<>(gson);
        }
        @Provides @Singleton
        public AppFlowPresenter<BaseSwitcherActivity> providePresenter(Parcer<Object> floParcer) {
            return new AppFlowPresenter<>(floParcer);
        }
    }

    @Inject @Named("activity") protected EventBus mBus;
    @Inject protected ActionBarOwner mActionBarOwner;
    @Inject AppFlowPresenter<BaseSwitcherActivity> mAppFlowPresenter;

    @InjectView(R.id.main) protected FrameScreenSwitcherView mContainer;
    @InjectView(R.id.main_toolbar) protected Toolbar mToolbar;

    protected ActionBarOwner.MenuConfig mMenuConfig;

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

        setSupportActionBar(mToolbar);
        mActionBarOwner.takeView(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBus != null) mBus.unregister(this);

        if (mAppFlowPresenter != null) mAppFlowPresenter.dropView(this);
        if (mActionBarOwner != null) mActionBarOwner.dropView(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mMenuConfig != null) {
            for (int item : mMenuConfig.menus) {
                getMenuInflater().inflate(item, menu);
            }
            for (CustomMenuItem item : mMenuConfig.customMenus) {
                menu.add(item.groupId, item.itemId, item.order, item.title)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                if (item.iconRes >= 0) {
                    menu.findItem(item.itemId)
                            .setIcon(item.iconRes)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMenuConfig != null
                && mMenuConfig.actionHandler != null
                && mMenuConfig.actionHandler.call(item.getItemId())) {
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                return mContainer.onUpPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mContainer.onBackPressed()) {
            super.onBackPressed();
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
    public void onEventMainThread(OpenAddToPlaylist e) {
        AddToPlaylistDialog.newInstance(e.songsToAdd)
                .show(getSupportFragmentManager(), "AddToPlaylistDialog");
    }

    public void onEventMainThread(ConfirmDelete e) {
        DeleteDialog.newInstance((String) e.title, e.songids, null) //TODO
                .show(getSupportFragmentManager(), "DeleteDialog");
    }

    /*
     * AppFlowPresenter.Activity
     */

    @Override
    public void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback) {
        mContainer.showScreen(screen, direction, callback);
    }

    /*
     * ActionBarOwner.Activity
     */

    @Override
    public void setUpButtonEnabled(boolean enabled) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
    }

    @Override
    public void setTitle(int titleId) {
        getSupportActionBar().setTitle(titleId);
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void setSubtitle(int subTitleRes) {
        getSupportActionBar().setSubtitle(subTitleRes);
    }

    @Override
    public void setSubtitle(CharSequence title) {
        getSupportActionBar().setSubtitle(title);
    }

    @Override
    public void setMenu(ActionBarOwner.MenuConfig menuConfig) {
        mMenuConfig = menuConfig;
        supportInvalidateOptionsMenu();
    }
}
