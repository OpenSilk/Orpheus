/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 */
public class HomeFragment extends Fragment {

    /** Default order containing all page fragments */
    public static final String[] DEFAULT_PAGES;
    /** Page titles for all fragments in DEFAULT_PAGES */
    public static final int[] PAGE_TITLES;
    /** Maps fragment class names to their title resource id */
    public static final Map<String, Integer> TITLE_MAP;

    static {
        DEFAULT_PAGES = new String[] {
                HomePlaylistFragment.class.getName(),
                HomeRecentFragment.class.getName(),
                HomeArtistFragment.class.getName(),
                HomeAlbumFragment.class.getName(),
                HomeSongFragment.class.getName(),
                HomeGenreFragment.class.getName(),
        };
        PAGE_TITLES = new int[] {
                R.string.page_playlists,
                R.string.page_recent,
                R.string.page_artists,
                R.string.page_albums,
                R.string.page_songs,
                R.string.page_genres,
        };
        TITLE_MAP = map(DEFAULT_PAGES, PAGE_TITLES);
    }

    private ViewPager mViewPager;
    private HomePagerAdapter mPagerAdapter;
    private PreferenceUtils mPreferences;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(getActivity());

        // Initialize the adapter
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());

        List<String> pages = mPreferences.getHomePages();

        if (pages == null || pages.size() < 1) {
            for (String className : DEFAULT_PAGES) {
                mPagerAdapter.add(className, null);
            }
        } else {
            for (String page : pages) {
                mPagerAdapter.add(page, null);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup)inflater.inflate(
                R.layout.pager_fragment, container, false);

        // Initialize the ViewPager
        mViewPager = (ViewPager)rootView.findViewById(R.id.pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
//        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mPreferences.getStartPage());
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Disable home as up
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        activity.getSupportActionBar().setHomeButtonEnabled(false);
        // Display title
        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
        activity.getSupportActionBar().setTitle(R.string.app_name);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the last page the use was on
        mPreferences.setStartPage(mViewPager.getCurrentItem());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewPager = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPagerAdapter = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Shuffle all
        inflater.inflate(R.menu.shuffle, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(getActivity());
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a map from key/value arrays
     * @param keys
     * @param values
     * @return
     */
    public static Map<String, Integer> map(String[] keys, int[] values) {
        if (keys.length != values.length) {
            return null;
        }
        Map<String, Integer> titleMap = new ArrayMap<>(keys.length);
        for (int ii=0; ii<keys.length; ii++) {
            titleMap.put(keys[ii], values[ii]);
        }
        return titleMap;
    }

    /**
     * Pager adapter
     */
    public static final class HomePagerAdapter extends FragmentPagerAdapter {
        private final Context mContext;
        private final List<Holder> mHolderList = Lists.newArrayList();

        public HomePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            Holder holder = mHolderList.get(position);
            return Fragment.instantiate(mContext, holder.className, holder.params);
        }

        @Override
        public int getCount() {
            return mHolderList.size();
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            final String className = mHolderList.get(position).className;
            return mContext.getString(TITLE_MAP.get(className)).toUpperCase(Locale.getDefault());
        }

        public void add(final String className, final Bundle params) {
            mHolderList.add(new Holder(className, params));
        }

        /**
         * A private class with information about fragment initialization
         */
        private final static class Holder {
            String className;
            Bundle params;
            private Holder(String className, Bundle params) {
                this.className = className;
                this.params = params;
            }
        }
    }

}