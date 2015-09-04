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

package org.opensilk.common.ui.mortar;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPresenter;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.HasScope;
import org.opensilk.common.ui.R;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
public class ToolbarOwnerDelegate<S extends HasScope> implements ToolbarOwnerScreen {

    private final S scope;
    private final ToolbarOwner toolbarOwner;
    private final Toolbar toolbar;

    ActionBarMenuConfig currentMenu;

    public ToolbarOwnerDelegate(
            S scope,
            ToolbarOwner toolbarOwner,
            Toolbar toolbar
    ) {
        this.scope = scope;
        this.toolbarOwner = toolbarOwner;
        this.toolbar = toolbar;
    }

    /*
     * Start lifecycle methods
     */

    public void onCreate() {
        toolbarOwner.takeView(this);
    }

    public void onDestroy() {
        toolbarOwner.dropView(this);
        currentMenu = null;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
//        if (currentMenu != null) {
//            for (int item : currentMenu.menus) {
//                activity.getMenuInflater().inflate(item, menu);
//            }
//            for (ActionBarMenuConfig.CustomMenuItem item : currentMenu.customMenus) {
//                menu.add(item.groupId, item.itemId, item.order, item.title)
//                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//                if (item.iconRes >= 0) {
//                    menu.findItem(item.itemId)
//                            .setIcon(item.iconRes)
//                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//                }
//            }
//            return true;
//        }
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
//        return currentMenu != null
//                && currentMenu.actionHandler != null
//                && currentMenu.actionHandler.call(activity, item.getItemId());
        return false;
    }

    /*
     * End lifecycle methods
     */

    @Override
    public void setUpButtonEnabled(boolean enabled) {
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
    }

    @Override
    public void setTitle(int titleId) {
        toolbar.setTitle(titleId);
    }

    @Override
    public void setTitle(CharSequence title) {
        toolbar.setTitle(title);
    }

    @Override
    public void setSubtitle(int titleId) {
        toolbar.setSubtitle(titleId);
    }

    @Override
    public void setSubtitle(CharSequence title) {
        toolbar.setSubtitle(title);
    }

    @Override
    public void setMenu(ActionBarMenuConfig menuConfig) {
        currentMenu = menuConfig;
    }

    public void clearMenu() {
        toolbar.getMenu().clear();
    }

    @Override
    public void setTransparentActionbar(boolean yes) {
        toolbar.getBackground().setAlpha(yes ? 0 : 255);
    }

    @Override
    public MortarScope getScope() {
        return scope.getScope();
    }

}
