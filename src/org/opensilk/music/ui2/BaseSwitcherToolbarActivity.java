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

import org.opensilk.music.R;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import javax.inject.Inject;

import butterknife.InjectView;

/**
 * Created by drew on 11/20/14.
 */
public class BaseSwitcherToolbarActivity extends BaseSwitcherActivity implements
        ActionBarOwner.Activity {


    @dagger.Module(
            includes = {
                    BaseSwitcherActivity.Module.class,
                    ActionBarOwner.Module.class,
            }
    )
    public static class Module {

    }

    @Inject protected ActionBarOwner mActionBarOwner;

    @InjectView(R.id.main_toolbar) protected Toolbar mToolbar;

    protected ActionBarOwner.MenuConfig mMenuConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(mToolbar);
        mActionBarOwner.takeView(this);
    }

    @Override
    protected void onDestroy() {
        if (mActionBarOwner != null) mActionBarOwner.dropView(this);
        mMenuConfig = null;
        super.onDestroy();
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

    @Override
    public void setTransparentActionbar(boolean yes) {
        mToolbar.getBackground().setAlpha(yes ? 0 : 255);
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
