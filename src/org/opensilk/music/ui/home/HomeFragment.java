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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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

import org.opensilk.music.ui.settings.DragSortSwipeListPreference;

import java.util.List;
import java.util.Locale;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 */
public class HomeFragment extends Fragment {

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

        List<String> pages = DragSortSwipeListPreference.listFromString(
                mPreferences.getHomePages());

        if (pages == null) {
            final MusicFragments[] mFragments = MusicFragments.values();
            for (final MusicFragments mFragment : mFragments) {
                mPagerAdapter.add(mFragment.getFragmentClass(),
                        getHumanReadable(mFragment.getFragmentClass().getName()), null);
            }
        } else {
            for (String page : pages) {
                Log.d("HomeFragment", "Pages: " + pages);
                mPagerAdapter.add(page,getHumanReadable(page), null);
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

    public static final class HomePagerAdapter extends FragmentPagerAdapter {
        private final Context context;
        private final List<Holder> mHolderList = Lists.newArrayList();


        public HomePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            Holder holder = mHolderList.get(position);
            Log.d("HomeFragment", "className: " + holder.className + " -- Position: [" + position + "]");
            return Fragment.instantiate(context, holder.className, holder.params);
        }

        @Override
        public int getCount() {
            return mHolderList.size();
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            return mHolderList.get(position).title.toUpperCase(Locale.getDefault());
        }

        public void add(final Class<? extends Fragment> className, String title, final Bundle params) {
            mHolderList.add(new Holder(className.getName(), title, params));
            Log.d("HomeFragment", "className: " + className.getName() + " -- Position: [" + mHolderList.size() + "]");
        }

        public void add(final String className, String title, final Bundle params) {
            mHolderList.add(new Holder(className, title, params));
            Log.d("HomeFragment", "className: " + className + " -- Position: [" + mHolderList.size() + "]");
        }


        public void removeAll() {
            mHolderList.clear();
        }

        /**
         * A private class with information about fragment initialization
         */
        private final static class Holder {
            String className;
            String title;
            Bundle params;
            private Holder(String className, String title, Bundle params) {
                this.className = className;
                this.title = title;
                this.params = params;
            }
        }
    }

    private String getHumanReadable(String className) {
        int id;
        if (className.equals(HomePlaylistFragment.class.getName())) {
            id = R.string.page_playlists;
        } else if (className.equals(HomeRecentFragment.class.getName())) {
            id = R.string.page_recent;
        } else if (className.equals(HomeArtistFragment.class.getName())) {
            id = R.string.page_artists;
        } else if (className.equals(HomeAlbumFragment.class.getName())) {
            id = R.string.page_albums;
        } else if (className.equals(HomeSongFragment.class.getName())) {
            id = R.string.page_songs;
        } else if (className.equals(HomeGenreFragment.class.getName())) {
            id = R.string.page_genres;
        } else {
            id = R.string.error;
        }

        return getResources().getString(id);
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public enum MusicFragments {
        /**
         * The playlist fragment
         */
        PLAYLIST(HomePlaylistFragment.class),
        /**
         * The recent fragment
         */
        RECENT(HomeRecentFragment.class),
        /**
         * The artist fragment
         */
        ARTIST(HomeArtistFragment.class),
        /**
         * The album fragment
         */
        ALBUM(HomeAlbumFragment.class),
        /**
         * The song fragment
         */
        SONG(HomeSongFragment.class),
        /**
         * The genre fragment
         */
        GENRE(HomeGenreFragment.class);

        private Class<? extends Fragment> mFragmentClass;

        /**
         * Constructor of <code>MusicFragments</code>
         *
         * @param fragmentClass The fragment class
         */
        private MusicFragments(final Class<? extends Fragment> fragmentClass) {
            mFragmentClass = fragmentClass;
        }

        /**
         * Method that returns the fragment class.
         *
         * @return Class<? extends Fragment> The fragment class.
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }
    }

}