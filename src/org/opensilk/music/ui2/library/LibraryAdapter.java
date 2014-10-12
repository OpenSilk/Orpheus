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

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.opensilk.music.api.model.spi.Bundleable;

/**
 * Created by drew on 10/10/14.
 */
public class LibraryAdapter extends ArrayAdapter<Bundleable> {

    private static class LoadingItem implements Bundleable {
        @Override
        public Bundle toBundle() {
            return null;
        }

        @Override
        public String toString() {
            return "Loading";
        }
    }

    final LibraryScreen.Presenter presenter;
    LibraryLoader.Result lastResult;

    final LoadingItem loadingItem;
    boolean hasLoadingItem = false;

    public LibraryAdapter(Context context, LibraryScreen.Presenter presenter) {
        super(context, android.R.layout.simple_list_item_1);
        this.presenter = presenter;
        loadingItem = new LoadingItem();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        maybeGetMore(position);
        return super.getView(position, convertView, parent);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof LoadingItem) ? 1 : 0;
    }

    public void onNewResult(LibraryLoader.Result result) {
        lastResult = result;
        if (hasLoadingItem) {
            remove(loadingItem);
            hasLoadingItem = false;
        }
        if (!lastResult.items.isEmpty()) addAll(lastResult.items);
    }

    private boolean maybeGetMore(int position) {
        if (hasLoadingItem) return false;
        if (lastResult == null || lastResult.token == null) return false ;
        if (getCount() == 0 || (getCount()-1) != position) return false ;
        if (presenter.fetchMore(lastResult.token)) {
            hasLoadingItem = true;
            add(loadingItem);
            return true;
        }
        return false;
    }

}
