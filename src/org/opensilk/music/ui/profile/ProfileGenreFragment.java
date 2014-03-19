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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.Lists;

import java.util.List;

/**
 * Created by drew on 2/28/14.
 */
public class ProfileGenreFragment extends Fragment {

    private ViewPager mViewPager;
    private GenrePagerAdapter mPagerAdapter;

    public static ProfileGenreFragment newInstance(Bundle args) {
        ProfileGenreFragment f = new ProfileGenreFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        // Initialize the adapter
        mPagerAdapter = new GenrePagerAdapter(getActivity(), getChildFragmentManager());
        mPagerAdapter.add(ProfileGenreAlbumsFragment.class.getName(), args);
        mPagerAdapter.add(ProfileGenreSongsFragment.class.getName(), args);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.profile_genre_pager, container, false);
        // Initialize the ViewPager
        mViewPager = (ViewPager)rootView.findViewById(R.id.genre_pager);
        // Attach the adapter
        mViewPager.setAdapter(mPagerAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewPager = null;
    }

    private class GenrePagerAdapter extends FragmentPagerAdapter {

        private final Context mContext;
        private final List<Holder> mHolderList = Lists.newArrayList();

        public GenrePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        String[] mPageTitles = new String[] {"Albums", "Songs"};

        public void add(String className, Bundle args) {
            final Holder holder = new Holder();
            holder.className = className;
            holder.args = args;
            mHolderList.add(holder);
        }

        @Override
        public Fragment getItem(int position) {
            final Holder holder = mHolderList.get(position);
            final Fragment f = Fragment.instantiate(mContext, holder.className, holder.args);
            return f;
        }

        @Override
        public int getCount() {
            return mHolderList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageTitles[position];
        }

        private final class Holder {
            String className;
            Bundle args;
        }
    }
}
