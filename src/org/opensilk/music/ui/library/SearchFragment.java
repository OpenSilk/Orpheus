/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.library;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import org.opensilk.music.R;

import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.ui.home.CardListGridFragment;
import org.opensilk.music.ui.library.adapter.LibraryAdapter;
import org.opensilk.music.ui.library.adapter.SearchAdapter;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

/**
 * Created by drew on 7/22/14.
 */
public class SearchFragment extends CardListGridFragment implements
        LibraryAdapter.Callback, SearchView.OnQueryTextListener {

    @Inject @ForActivity
    protected ActionBarController mActionBarHelper;
    @Inject @ForActivity
    protected DrawerHelper mDrawerHelper;
    @Inject @ForFragment
    protected RemoteLibraryHelper mLibrary;

    protected SearchView mSearchView;

    protected LibraryInfo mLibraryInfo;
    protected SearchAdapter mAdapter;

    public static SearchFragment newInstance(LibraryInfo info) {
        SearchFragment f = new SearchFragment();
        Bundle b = new Bundle();
        b.putParcelable(LibraryFragment.ARG_LIBRARY_INFO, info);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) getParentFragment()).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLibraryInfo = getArguments().getParcelable(LibraryFragment.ARG_LIBRARY_INFO);
        mAdapter = new SearchAdapter(getActivity(), mLibrary, mLibraryInfo, this, (DaggerInjector) getParentFragment());
        if (savedInstanceState != null) {
            mAdapter.restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //TODO set empty text
        super.onViewCreated(view, savedInstanceState);
        setListAdapter((ArrayAdapter) mAdapter);
        setListShown(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mDrawerHelper.isDrawerOpen()) {
            menu.removeItem(R.id.menu_search);
            // Search view
            inflater.inflate(R.menu.searchview, menu);
            MenuItem searchItem = menu.findItem(R.id.menu_searchview);
            mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setIconified(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if (isViewCreated()) {
            setListShown(false);
        }
        mAdapter.query(s);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    /*
     * LibraryAdapter.Callback
     */

    @Override
    public void onFirstLoadComplete() {
        if (isViewCreated()) {
            setListShown(true);
        }
    }

    @Override
    public void onLoadingFailure(boolean relaunchPicker) {
        if (relaunchPicker) {
            ((LibraryFragment) getParentFragment()).relaunchLibraryFragment();
        } else {
            //TODO show error
            if (isViewCreated()) {
                setListShown(true);
            }
        }
    }

    /*
     * Abstract methods
     */

    @Override
    public int getListViewLayout() {
        return R.layout.card_listview;
    }

    @Override
    public int getEmptyViewLayout() {
        return R.layout.list_empty_view;
    }

}
