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

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.ui.mortar.ActionBarOwner;
import org.opensilk.common.ui.mortar.ActionBarOwnerDelegate;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 5/1/15.
 */
public abstract class MusicActivityToolbar extends MusicActivity {

    @Inject protected ActionBarOwner mActionBarOwner;

    /*@InjectView(R.id.main_toolbar)*/ protected Toolbar mToolbar;

    protected ActionBarOwnerDelegate<MusicActivityToolbar> mActionBarDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToolbar = ButterKnife.findById(this, R.id.main_toolbar);
        mActionBarDelegate = new ActionBarOwnerDelegate<>(this, mActionBarOwner, mToolbar);
        mActionBarDelegate.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActionBarDelegate.onDestroy();
    }

    /*
     * Action bar owner
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mActionBarDelegate.onCreateOptionsMenu(menu) || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarDelegate.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

}
