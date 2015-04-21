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

package org.opensilk.music.ui2.search;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;

import org.opensilk.music.ui2.SearchActivity;
import org.opensilk.music.widgets.RecyclerListFrame;

import javax.inject.Inject;

import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 11/24/14.
 */
public class SearchListView extends RecyclerListFrame implements
        SearchView.OnQueryTextListener,
        SearchViewOwner.View {

    @Inject SearchScreenPresenter presenter;
    @Inject SearchViewOwner searchViewOwner;

    SearchAdapter adapter;

    SearchView searchView;
    String filterString;


    public SearchListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new SearchAdapter(getContext());
        mList.setAdapter(adapter);
        mList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        searchViewOwner.takeView(this);
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        searchViewOwner.dropView(this);
    }

    /*
     * implement QueryTextListener
     */

    @Override
    public boolean onQueryTextSubmit(final String query) {
        if (TextUtils.isEmpty(query)) return false;
        hideKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        if (TextUtils.isEmpty(newText)) return false;
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        adapter.clear();
        setLoading(true);
        presenter.restartLoaders(newText);
        return true;
    }

    /*
     * implement SearchViewOwner.View
     */

    @Override
    public void onSearchViewCreated(SearchView searchView) {
        this.searchView = searchView;
        this.searchView.setOnQueryTextListener(this);
        this.searchView.setIconified(false);
        if (!TextUtils.isEmpty(filterString)) this.searchView.setQuery(filterString, false);
        // Add voice search
        final SearchManager searchManager = (SearchManager)getContext().getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(new ComponentName(getContext(), SearchActivity.class));
        this.searchView.setSearchableInfo(searchableInfo);
    }

    @Override
    public void onNewQuery(String query) {
        filterString = query;
        if (!TextUtils.isEmpty(query)) {
            adapter.clear();
            setLoading(true);
            presenter.restartLoaders(query);
            if (searchView != null) searchView.setQuery(query, false);
            hideKeyboard();
        }
    }

    @Override
    public MortarScope getScope() {
        return Mortar.getScope(getContext());
    }

    void hideKeyboard() {
        // When the search is "committed" by the user, then hide the keyboard so
        // the user can more easily browse the list of results.
        if (searchView != null) {
            final InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
            searchView.clearFocus();
        }
    }
}
