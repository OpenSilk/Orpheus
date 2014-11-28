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

import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.music.R;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.nowplaying.NowPlayingScreen;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import flow.Layouts;

/**
 * Created by drew on 11/17/14.
 */
public class NowPlayingActivity extends BaseMortarActivity
        implements  ActionBarOwner.Activity {

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
            includes = {
                    BaseMortarActivity.Module.class,
                    ActionBarOwner.Module.class,
            },
            injects = NowPlayingActivity.class
    )
    public static class Module {
    }

    @Inject protected ActionBarOwner mActionBarOwner;
    @InjectView(R.id.now_playing_toolbar) Toolbar mToolbar;
    protected ActionBarOwner.MenuConfig mMenuConfig;

    @Override
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        OrpheusTheme orpheusTheme = mSettings.getTheme();
        setTheme(mSettings.isDarkTheme() ? orpheusTheme.dark : orpheusTheme.light);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MortarContextFactory contextFactory = new MortarContextFactory();
        setContentView(Layouts.createView(contextFactory.setUpContext(new NowPlayingScreen(), this), NowPlayingScreen.class));
        ButterKnife.inject(this);
        setSupportActionBar(mToolbar);
        mActionBarOwner.takeView(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mActionBarOwner != null) mActionBarOwner.dropView(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        populateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleOptionItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    /*
     * ActionBarOwner.Activity
     */

    @Override
    public void setUpButtonEnabled(boolean enabled) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    @Override
    public void setTransparentActionbar(boolean yes) {
//        mToolbar.getBackground().setAlpha(yes ? 0 : 255);
    }

    protected void populateOptionsMenu(Menu menu) {
        if (mMenuConfig != null) {
            for (int item : mMenuConfig.menus) {
                getMenuInflater().inflate(item, menu);
            }
            for (ActionBarOwner.CustomMenuItem item : mMenuConfig.customMenus) {
                menu.add(item.groupId, item.itemId, item.order, item.title)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                if (item.iconRes >= 0) {
                    menu.findItem(item.itemId)
                            .setIcon(item.iconRes)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }
    }

    protected boolean handleOptionItemSelected(MenuItem item) {
        return mMenuConfig != null
                && mMenuConfig.actionHandler != null
                && mMenuConfig.actionHandler.call(item.getItemId());
    }
}
