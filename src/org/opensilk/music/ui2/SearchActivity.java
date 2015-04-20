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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.music.R;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui2.library.PluginConnectionManager;
import org.opensilk.music.ui2.search.SearchScreen;
import org.opensilk.music.ui2.search.SearchViewOwner;

import javax.inject.Inject;

import static android.app.SearchManager.QUERY;

/**
 * Created by drew on 11/9/14.
 */
public class SearchActivity extends BaseSwitcherToolbarActivity {

    @Inject SearchViewOwner mSearchViewOwner;
    @Inject PluginConnectionManager mPluginConnectionManager;

    @Override
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new SearchActivityBlueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        OrpheusTheme orpheusTheme = mSettings.getTheme();
        setTheme(mSettings.isDarkTheme() ? orpheusTheme.dark : orpheusTheme.light);
    }

    @Override
    public Screen getDefaultScreen() {
        return new SearchScreen();
    }

    @Override
    protected void setupView() {
        setContentView(R.layout.main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setUpButtonEnabled(true);
//        getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        AppFlow.loadInitialScreen(this);

        if (savedInstanceState == null) {
            onNewIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null) return;
//        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mSearchViewOwner.notifyNewQuery(intent.getStringExtra(QUERY));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mConfigurationChangeIncoming) {
            // Release service connection
            mPluginConnectionManager.onDestroy();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPluginConnectionManager.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPluginConnectionManager.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        populateOptionsMenu(menu);
        // Search view
        MenuItem searchItem = menu.findItem(R.id.menu_searchview);
        if (searchItem != null) {
            mSearchViewOwner.notifySearchViewCreated(
                    (SearchView) MenuItemCompat.getActionView(searchItem)
            );
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return onSupportNavigateUp();
            default:
                return handleOptionItemSelected(item);
        }
    }

}
