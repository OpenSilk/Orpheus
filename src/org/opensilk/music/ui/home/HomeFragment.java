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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.music.R;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.ArtistCardClickHandler;
import org.opensilk.music.ui.cards.handler.GenreCardClickHandler;
import org.opensilk.music.ui.cards.handler.PlaylistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.home.adapter.HomePagerAdapter;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.common.widget.SlidingTabLayout;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment}
 */
public class HomeFragment extends ScopedDaggerFragment {

    @Inject @ForActivity
    DrawerHelper mDrawerHelper;
    @Inject @ForActivity
    ActionBarController mActionBarHelper;
    @Inject @ForFragment
    Bus mFragmentBus;
    @Inject
    AppPreferences mSettings;

    @InjectView(R.id.pager)
    ViewPager mViewPager;
    @InjectView(R.id.tab_bar)
    SlidingTabLayout mTabs;

    private HomePagerAdapter mPagerAdapter;

    protected AlbumCardClickHandler mAlbumHandler;
    protected ArtistCardClickHandler mArtistHandler;
    protected GenreCardClickHandler mGenreHandler;
    protected PlaylistCardClickHandler mPlaylistHandler;
    protected SongCardClickHandler mSongHandler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the adapter
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());

        List<MusicFragment> pages = mSettings.getHomePages();

        if (pages == null || pages.size() < 1) {
            final MusicFragment[] mFragments = MusicFragment.values();
            for (final MusicFragment mFragment : mFragments) {
                mPagerAdapter.add(mFragment, null);
            }
        } else {
            for (MusicFragment page : pages) {
                mPagerAdapter.add(page, null);
            }
        }

        registerHandlers();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup)inflater.inflate(
                R.layout.pager_fragment, container, false);
        ButterKnife.inject(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // attach tabs
        mTabs.setViewPager(mViewPager);
        // Offscreen pager loading limit
//        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mSettings.getInt(AppPreferences.START_PAGE, AppPreferences.DEFAULT_PAGE));
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Set title
        mActionBarHelper.setTitle(getString(R.string.my_library));
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save the last page the use was on
        mSettings.putInt(AppPreferences.START_PAGE, mViewPager.getCurrentItem());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        unregisterHandlers();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mDrawerHelper.isDrawerOpen()) {
            //Set title (hacks)
            mActionBarHelper.setTitle(getString(R.string.my_library));
            // search
            inflater.inflate(R.menu.search, menu);
            // Party shuffle
            inflater.inflate(R.menu.party_shuffle, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                NavUtils.openSearch(getActivity());
                return true;
            case R.id.menu_party_shuffle:
                // Starts autoshuffle mode
                MusicUtils.startPartyShuffle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Abstract methods
     */

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new HomeModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
    }

    private void registerHandlers() {
        mAlbumHandler = getObjectGraph().get(AlbumCardClickHandler.class);
        mArtistHandler = getObjectGraph().get(ArtistCardClickHandler.class);
        mGenreHandler = getObjectGraph().get(GenreCardClickHandler.class);
        mPlaylistHandler = getObjectGraph().get(PlaylistCardClickHandler.class);
        mSongHandler = getObjectGraph().get(SongCardClickHandler.class);
        mFragmentBus.register(mAlbumHandler);
        mFragmentBus.register(mArtistHandler);
        mFragmentBus.register(mGenreHandler);
        mFragmentBus.register(mPlaylistHandler);
        mFragmentBus.register(mSongHandler);
    }

    private void unregisterHandlers() {
        mFragmentBus.unregister(mAlbumHandler);
        mFragmentBus.unregister(mArtistHandler);
        mFragmentBus.unregister(mGenreHandler);
        mFragmentBus.unregister(mPlaylistHandler);
        mFragmentBus.unregister(mSongHandler);
    }

}