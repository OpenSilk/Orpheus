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

package org.opensilk.music.ui.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.music.adapters.PagerAdapter;
import org.opensilk.music.ui.fragments.SearchFragment;
import org.opensilk.music.ui.home.HomeAlbumFragment;
import org.opensilk.music.ui.home.HomeArtistFragment;
import org.opensilk.music.ui.home.HomeSongFragment;
import org.opensilk.music.ui.settings.SettingsActivity;

import static android.app.SearchManager.QUERY;
import static org.opensilk.music.util.ConfigHelper.isLargeLandscape;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeSlidingActivity extends BaseSlidingActivity {
    private static final String TAG = HomeSlidingActivity.class.getSimpleName();

    public static final int RESULT_RESTART_APP = RESULT_FIRST_USER << 1;

    /** Pager */
    private ViewPager mViewPager;

    /** VP's adapter */
    private PagerAdapter mPagerAdapter;

    private PreferenceUtils mPreferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(this);

        // Initialize the adapter
        mPagerAdapter = new PagerAdapter(this, getSupportFragmentManager());
        final PagerAdapter.MusicFragments[] mFragments = PagerAdapter.MusicFragments.values();
        for (final PagerAdapter.MusicFragments mFragment : mFragments) {
            mPagerAdapter.add(mFragment.getFragmentClass(), null);
        }

        // Initialize the ViewPager
        mViewPager = (ViewPager) findViewById(R.id.home_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mPreferences.getStartPage());

        if (savedInstanceState == null) {
            restoreActionBar();
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                hidePager();
            } else {
                restoreActionBar();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(QUERY);
            if (!TextUtils.isEmpty(query)) {
                SearchFragment f = (SearchFragment) getSupportFragmentManager().findFragmentByTag("search");
                if (f != null) {
                    f.onNewQuery(query);
                }
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the last page the use was on
        mPreferences.setStartPage(mViewPager.getCurrentItem());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager = null;
        mPagerAdapter = null;
    }

    @Override
    public void onBackPressed() {
        //If we're coming back to home
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            // show pager
            showPager();
            restoreActionBar();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // search option
        getMenuInflater().inflate(R.menu.search, menu);
        // Don't show pager items on profiles or search
        if (mViewPager.getVisibility() == View.VISIBLE) {
            // Shuffle all
            getMenuInflater().inflate(R.menu.shuffle, menu);
            // Sort orders
            if (isRecentPage()) {
                getMenuInflater().inflate(R.menu.view_as, menu);
            } else if (isArtistPage()) {
                getMenuInflater().inflate(R.menu.artist_sort_by, menu);
                getMenuInflater().inflate(R.menu.view_as, menu);
            } else if (isAlbumPage()) {
                getMenuInflater().inflate(R.menu.album_sort_by, menu);
                getMenuInflater().inflate(R.menu.view_as, menu);
            } else if (isSongPage()) {
                getMenuInflater().inflate(R.menu.song_sort_by, menu);
            }
            // Settings
            getMenuInflater().inflate(R.menu.settings, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(this);
                return true;
            case R.id.menu_favorite:
                // Toggle the current track as a favorite and update the menu
                // item
                MusicUtils.toggleFavorite();
                invalidateOptionsMenu();
                return true;
            case R.id.menu_sort_by_az:
                if (isArtistPage()) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_za:
                if (isArtistPage()) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_artist:
                if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_album:
                if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_year:
                if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                    getAlbumFragment().refresh();
                } else if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_duration:
                if (isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_number_of_songs:
                if (isArtistPage()) {
                    mPreferences
                            .setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                    getArtistFragment().refresh();
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                    getAlbumFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_number_of_albums:
                if (isArtistPage()) {
                    mPreferences
                            .setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                    getArtistFragment().refresh();
                }
                return true;
            case R.id.menu_sort_by_filename:
                if(isSongPage()) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                    getSongFragment().refresh();
                }
                return true;
            case R.id.menu_view_as_simple:
                if (isRecentPage()) {
                    mPreferences.setRecentLayout("simple");
                } else if (isArtistPage()) {
                    mPreferences.setArtistLayout("simple");
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumLayout("simple");
                }
                NavUtils.goHome(this);
                return true;
            case R.id.menu_view_as_grid:
                if (isRecentPage()) {
                    mPreferences.setRecentLayout("grid");
                } else if (isArtistPage()) {
                    mPreferences.setArtistLayout("grid");
                } else if (isAlbumPage()) {
                    mPreferences.setAlbumLayout("grid");
                }
                NavUtils.goHome(this);
                return true;
            case R.id.menu_search:
                NavUtils.openSearch(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_RESTART_APP) {
            // Hack to force a refresh for our activity for eg theme change
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                    getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+700, pi);
            finish();
        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        //Dont hide action bar on tablets
        if (!isLargeLandscape(getResources())) {
            super.onPanelSlide(panel, slideOffset);
        }
    }

    @Override
    protected void maybeHideActionBar() {
        //Dont hide action bar on tablets
        if (!isLargeLandscape(getResources())) {
            super.maybeHideActionBar();
        }
    }

    /**
     * Resets action bar to default state
     */
    private void restoreActionBar() {
        // Disable home as up
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        // Display title
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
    }

    /**
     * Hides viewpager in portrait to avoid showing behind current fragement
     */
    public void hidePager() {
        if (!isLargeLandscape(getResources())) {
            mViewPager.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Restores viewpagers visiblity
     */
    public void showPager() {
        mViewPager.setVisibility(View.VISIBLE);
    }

    private boolean isArtistPage() {
        return mViewPager.getCurrentItem() == 2;
    }

    private HomeArtistFragment getArtistFragment() {
        return (HomeArtistFragment)mPagerAdapter.getFragment(2);
    }

    private boolean isAlbumPage() {
        return mViewPager.getCurrentItem() == 3;
    }

    private HomeAlbumFragment getAlbumFragment() {
        return (HomeAlbumFragment)mPagerAdapter.getFragment(3);
    }

    private boolean isSongPage() {
        return mViewPager.getCurrentItem() == 4;
    }

    private HomeSongFragment getSongFragment() {
        return (HomeSongFragment)mPagerAdapter.getFragment(4);
    }

    private boolean isRecentPage() {
        return mViewPager.getCurrentItem() == 1;
    }
}
