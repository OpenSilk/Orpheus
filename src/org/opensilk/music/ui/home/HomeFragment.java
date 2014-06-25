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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.SongCardEvent;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import java.util.List;
import java.util.Locale;

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
    @InjectView(R.id.pager)
    ViewPager mViewPager;
    private HomePagerAdapter mPagerAdapter;
    private PreferenceUtils mPreferences;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the preferences
        mPreferences = PreferenceUtils.getInstance(getActivity());

        // Initialize the adapter
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());

        List<MusicFragment> pages = mPreferences.getHomePages();

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

        mFragmentBus.register(this);
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
        // Offscreen pager loading limit
//        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(mPreferences.getStartPage());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        //Set title
        mActionBarHelper.setTitle(getString(R.string.drawer_device));
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
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        mPagerAdapter = null;
        mFragmentBus.unregister(this);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mDrawerHelper.isDrawerOpen()) {
            // Party shuffle
            inflater.inflate(R.menu.party_shuffle, menu);
            // Shuffle all
//        inflater.inflate(R.menu.shuffle, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_party_shuffle:
                // Starts autoshuffle mode
                MusicUtils.startPartyShuffle();
                return true;
            case R.id.menu_shuffle:
                // Shuffle all the songs
                MusicUtils.shuffleAll(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Eventes
     */

    @Subscribe
    public void onSongPopupItemClicked(SongCardEvent e) {
        switch (e.event) {
            case PLAY_NEXT:
                if (e.song.isLocal()) {
                    try {
                        long id = Long.decode(e.song.identity);
                        MusicUtils.playNext(new long[] {id});
                    } catch (NumberFormatException ex) {
                        //TODO
                    }
                } else {
                    //TODO
                }
                break;
            case ADD_TO_QUEUE:
                if (e.song.isLocal()) {
                    try {
                        long id = Long.decode(e.song.identity);
                        MusicUtils.addToQueue(getActivity(), new long[] {id});
                    } catch (NumberFormatException ex) {
                        //TODO
                    }
                } else {
                    //TODO
                }
                break;
            case ADD_TO_PLAYLIST:
                if (e.song.isLocal()) {
                    try {
                        long id = Long.decode(e.song.identity);
                        AddToPlaylistDialog.newInstance(new long[]{id})
                                .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    } catch (NumberFormatException ex) {
                        //TODO
                    }
                } // else unsupported
                break;
            case MORE_BY_ARTIST:
                if (e.song.isLocal()) {
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), e.song.artistName));
                } else {
                    //TODO
                }
                break;
            case SET_RINGTONE:
                if (e.song.isLocal()) {
                    try {
                        long id = Long.decode(e.song.identity);
                        MusicUtils.setRingtone(getActivity(), id);
                    } catch (NumberFormatException ex) {
                        //TODO
                    }
                } // else unsupported
                break;
            case DELETE:
                if (e.song.isLocal()) {
                    try {
                        long id = Long.decode(e.song.identity);
                        DeleteDialog.newInstance(e.song.name, new long[]{ id }, null)
                                .show(getChildFragmentManager(), "DeleteDialog");
                    } catch (NumberFormatException ex) {
                        //TODO
                    }
                } // else unsupported
                break;
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

    /**
     * Pager adapter
     */
    public static final class HomePagerAdapter extends FragmentPagerAdapter {
        private final Context mContext;
        private final List<FragmentHolder> mHolderList = Lists.newArrayList();

        public HomePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            FragmentHolder holder = mHolderList.get(position);
            return Fragment.instantiate(mContext, holder.fragment.getFragmentClass().getName(), holder.params);
        }

        @Override
        public int getCount() {
            return mHolderList.size();
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            final int id = mHolderList.get(position).fragment.getTitleResource();
            return mContext.getString(id).toUpperCase(Locale.getDefault());
        }

        public void add(final MusicFragment fragment, final Bundle params) {
            mHolderList.add(new FragmentHolder(fragment, params));
        }

        /**
         * A private class with information about fragment initialization
         */
        private final static class FragmentHolder {
            MusicFragment fragment;
            Bundle params;
            private FragmentHolder(MusicFragment fragment, Bundle params) {
                this.fragment = fragment;
                this.params = params;
            }
        }
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public static enum MusicFragment {
        /**
         * The playlist fragment
         */
        PLAYLIST(HomePlaylistFragment.class, R.string.page_playlists),
        /**
         * The recent fragment
         */
        RECENT(HomeRecentFragment.class, R.string.page_recent),
        /**
         * The artist fragment
         */
        ARTIST(HomeArtistFragment.class, R.string.page_artists),
        /**
         * The album fragment
         */
        ALBUM(HomeAlbumFragment.class, R.string.page_albums),
        /**
         * The song fragment
         */
        SONG(SongFragment.class, R.string.page_songs),
        /**
         * The genre fragment
         */
        GENRE(HomeGenreFragment.class, R.string.page_genres);

        private Class<? extends Fragment> mFragmentClass;
        private int mTitleResource;

        /**
         * Constructor of <code>MusicFragments</code>
         *
         * @param fragmentClass The fragment class
         */
        private MusicFragment(final Class<? extends Fragment> fragmentClass,
                              final int titleResource) {
            mFragmentClass = fragmentClass;
            mTitleResource = titleResource;
        }

        /**
         * Method that returns the fragment class.
         *
         * @return Class<? extends Fragment> The fragment class.
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

        public int getTitleResource() {
            return mTitleResource;
        }

    }

}