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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreFragment extends Fragment {

    private ViewPager mViewPager;
    private ProfileGenrePagerAdapter mPagerAdapter;

    private Genre mGenre;

    public static ProfileGenreFragment newInstance(Bundle args) {
        ProfileGenreFragment f = new ProfileGenreFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mGenre = args.getParcelable(Config.EXTRA_DATA);
        // Initialize the adapter
        mPagerAdapter = new ProfileGenrePagerAdapter(this, mGenre);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.pager_fragment, container, false);
        // Initialize the ViewPager
        mViewPager = (ViewPager)rootView.findViewById(R.id.pager);
        // Attach the adapter
        mViewPager.setAdapter(mPagerAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // set actionbar title
        getActivity().getActionBar().setTitle(mGenre.mGenreName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewPager = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.card_genre, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.card_menu_play:
                MusicUtils.playAll(getActivity(), MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId), 0, false);
                return true;
            case R.id.card_menu_add_queue:
                MusicUtils.addToQueue(getActivity(), MusicUtils.getSongListForGenre(getActivity(), mGenre.mGenreId));
                return true;
        }
        return false;
    }

}
