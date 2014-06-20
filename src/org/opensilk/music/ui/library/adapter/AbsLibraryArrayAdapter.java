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

package org.opensilk.music.ui.library.adapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.music.api.model.Resource;
import org.opensilk.music.ui.library.card.AbsListCard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * Created by drew on 6/14/14.
 */
public abstract class AbsLibraryArrayAdapter<D extends Resource> extends CardArrayAdapter {

    public static final int STEP = 20;

    protected final String mLibraryIdentity;
    protected final ComponentName mLibraryComponent;

    protected Bundle mPaginationBundle;

    protected boolean mLoadingInProgress;
    protected boolean mEndOfResults;

    protected LoaderCallback mCallback;
    protected boolean mFirstLoadComplete;

    public interface LoaderCallback {
        public void onFirstLoadComplete();
    }

    protected AbsLibraryArrayAdapter(Context context, String libraryIdentity, ComponentName libraryComponent, LoaderCallback callback) {
        super(context, new ArrayList<Card>());
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("Context must be from activity");
        }
        mLibraryIdentity = libraryIdentity;
        mLibraryComponent = libraryComponent;
        mCallback = callback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        maybeLoadMore(position);
        return super.getView(position, convertView, parent);
    }

    public void startLoad() {
        mPaginationBundle = null;
        mFirstLoadComplete = false;
        clear();
        getMore();
    }

    protected void maybeLoadMore(int position) {
        if (!mLoadingInProgress && !mEndOfResults) {
            if ((getCount() - 1) == position) {
                getMore();
            }
        }
    }

    protected abstract void getMore();

    protected abstract void onSaveInstanceState(Bundle outState);
    protected abstract void onRestoreInstanceState(Bundle inState);

    protected abstract Card makeCard(D data);

    public void saveInstanceState(Bundle outState) {
        Bundle b = new Bundle();
        ArrayList<D> items = new ArrayList<>(getCount());
        for (int ii=0; ii<getCount(); ii++) {
            items.add(getItemData(ii));
        }
        b.putParcelableArrayList("items", items);
        b.putBundle("pagination", mPaginationBundle);
        b.putBoolean("eor", mEndOfResults);
        b.putBoolean("flc", mFirstLoadComplete);
        onSaveInstanceState(b);
        outState.putBundle(getClass().getName(), b);
    }

    public void restoreInstanceState(Bundle inState) {
        Bundle b = inState.getBundle(getClass().getName());
        if (b == null) {
            return;
        }
        if (getCount() > 0) {
            clear();
        }
        ArrayList<D> items = b.getParcelableArrayList("items");
        addItems(items);
        mPaginationBundle = b.getBundle("pagination");
        mEndOfResults = b.getBoolean("eor");
        mFirstLoadComplete = b.getBoolean("flc");
        onRestoreInstanceState(b);
    }

    public void addItems(Collection<D> collection) {
        List<Card> cards = new ArrayList<>(collection.size());
        for (D item : collection) {
            cards.add(makeCard(item));
        }
        addAll(cards);
    }

    public D getItemData(int position) {
        return ((AbsListCard<D>) getItem(position)).getData();
    }

    public boolean isOnFirstLoad() {
        return !mFirstLoadComplete;
    }

}
