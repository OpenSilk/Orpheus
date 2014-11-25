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

import android.support.v7.widget.SearchView;

import org.opensilk.common.mortar.HasScope;

import mortar.MortarScope;
import mortar.Presenter;

/**
 * Created by drew on 11/24/14.
 */
public class SearchViewOwner extends Presenter<SearchViewOwner.View> {

    public interface View extends HasScope {
        void onSearchViewCreated(SearchView searchView);
        void onNewQuery(String query);
    }

    @Override
    protected MortarScope extractScope(View view) {
        return view.getScope();
    }

    public void notifyNewQuery(String query) {
        if (getView() != null) {
            getView().onNewQuery(query);
        }
    }

    public void notifySearchViewCreated(SearchView searchView) {
        if (getView() != null) {
            getView().onSearchViewCreated(searchView);
        }
    }

}
