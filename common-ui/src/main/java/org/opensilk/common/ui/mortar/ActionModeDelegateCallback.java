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

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by drew on 10/1/15.
 */
public class ActionModeDelegateCallback implements ActionMode.Callback {

    private final ActionMode.Callback delegate;

    public ActionModeDelegateCallback(ActionMode.Callback delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return delegate.onCreateActionMode(mode, menu);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return delegate.onPrepareActionMode(mode, menu);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return delegate.onActionItemClicked(mode, item);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        delegate.onDestroyActionMode(mode);
    }
}
