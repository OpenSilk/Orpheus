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
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.HasScope;

import mortar.MortarScope;

/**
 * Created by drew on 5/5/15.
 */
public class ToolbarOwnerDelegate<A extends AppCompatActivity & HasScope> implements ToolbarOwnerActivity {

    private final A activity;
    private final ToolbarOwner toolbarOwner;
    private final Callback callback;

    ActionBarMenuHandler currentMenu;

    public interface Callback {
        void onToolbarAttached(Toolbar toolbar);

        void onToolbarDetached(Toolbar toolbar);
    }

    public ToolbarOwnerDelegate(A activity, ToolbarOwner toolbarOwner) {
        this(activity, toolbarOwner, null);
    }

    public ToolbarOwnerDelegate(A activity, ToolbarOwner toolbarOwner, Callback callback) {
        this.activity = activity;
        this.toolbarOwner = toolbarOwner;
        this.callback = callback;
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
        return currentMenu != null && currentMenu.onBuildMenu(activity.getMenuInflater(), menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return currentMenu != null && currentMenu.onMenuItemClicked(activity, item);
    }

    /*
     * End lifecycle methods
     */

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        activity.setSupportActionBar(toolbar);
    }

    @Override
    public void setToolbarMenu(ActionBarMenuHandler menuConfig) {
        currentMenu = menuConfig;
        activity.supportInvalidateOptionsMenu();
    }

    @Override
    public MortarScope getScope() {
        return activity.getScope();
    }

    @Override
    public void onToolbarAttached(Toolbar toolbar) {
        if (callback != null) {
            callback.onToolbarAttached(toolbar);
        }
    }

    @Override
    public void onToolbarDetached(Toolbar toolbar) {
        currentMenu = null;
        if (callback != null) {
            callback.onToolbarDetached(toolbar);
        }
    }
}
