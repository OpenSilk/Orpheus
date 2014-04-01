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

package org.opensilk.music.ui.profile;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;

import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 4/1/14.
 */
public abstract class ProfileGenrePageBase {

    // container
    protected final View mContainer;

    // main content
    protected final ListView mListView;

    // context
    protected final Fragment mHostFragment;

    // data
    protected final Genre mGenre;

    ProfileGenrePageBase(ProfileGenreFragment fragment, Genre genre, ViewGroup container) {
        mHostFragment = fragment;
        mGenre = genre;
        LayoutInflater inflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = inflater.inflate(R.layout.card_listview_topmargin, container, false);
        container.addView(mContainer);
        mListView = (CardListView) mContainer.findViewById(android.R.id.list);
    }

    public View getView() {
        return mContainer;
    }

    public abstract void finish();
}
