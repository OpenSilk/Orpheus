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

package org.opensilk.music.plugin.common;

import android.content.Context;

import com.commonsware.cwac.endless.EndlessAdapter;

/**
 * Created by drew on 7/19/14.
 */
public class LibraryEndlessAdapter extends EndlessAdapter {

    public LibraryEndlessAdapter(Context context, LibraryArrayAdapter wrapped) {
        super(context, wrapped, R.layout.adapter_loading_view);
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        getWrappedAdapter().loadMore();
        return getWrappedAdapter().endOfResults();
    }

    @Override
    protected void appendCachedData() {
        getWrappedAdapter().addPending();
    }

    @Override
    protected LibraryArrayAdapter getWrappedAdapter() {
        return (LibraryArrayAdapter) super.getWrappedAdapter();
    }
}
