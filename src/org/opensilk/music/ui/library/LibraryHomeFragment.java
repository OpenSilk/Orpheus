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

package org.opensilk.music.ui.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.Lists;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.Api;
import org.opensilk.music.api.PluginInfo;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.RemoteLibraryEvent;
import org.opensilk.music.util.RemoteLibraryUtil;

import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static org.opensilk.music.api.Api.Ability.BROWSE_FOLDERS;
import static org.opensilk.music.api.Api.Ability.QUERY_ALBUMS;
import static org.opensilk.music.api.Api.Ability.QUERY_ARTISTS;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryHomeFragment extends Fragment {

    public static final int REQUEST_LIBRARY = 1001;
    public static final String ARG_COMPONENT = "argComponent";
    public static final String ARG_IDENTITY = "argIdentity";

    private RemoteLibrary mLibraryService;
    private PluginInfo mPluginInfo;
    private String mLibraryIdentity;

    @InjectView(R.id.pager)
    protected ViewPager mPager;

    protected HomePagerAdapter mPagerAdapter;

    public static LibraryHomeFragment newInstance(PluginInfo p) {
        LibraryHomeFragment f = new LibraryHomeFragment();
        Bundle b = new Bundle(1);
        b.putParcelable("plugininfo", p);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPluginInfo = getArguments().getParcelable("plugininfo");
        // TODO get LibraryIdentity from prefs;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.pager_fragment, container, false);
        ButterKnife.inject(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        if (isLibraryBound()) {
            requestLibrary();
        }

        EventBus.getInstance().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
        EventBus.getInstance().unregister(this);
    }

    @Override
    @DebugLog
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LIBRARY:
                if (resultCode == Activity.RESULT_OK) {
                    final String id = data.getStringExtra(Api.EXTRA_LIBRARY_ID);
                    if (TextUtils.isEmpty(id)) {
                        throw new RuntimeException("Library chooser must set EXTRA_LIBRARY_ID");
                    }
                    mLibraryIdentity = id;
                    addPages();
                } else {
                    //TODO
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
     * Eventes
     */

    @Subscribe
    public void onServiceConnected(RemoteLibraryEvent.Bound e) {
        if (mPluginInfo.componentName.equals(e.componentName)) {
            requestLibrary();
        }
    }

    @Subscribe
    public void onServiceDisconnected(RemoteLibraryEvent.Unbound e) {
        if (mPluginInfo.componentName.equals(e.componentName)) {
            mLibraryService = null;
        }
    }

    private boolean isLibraryBound() {
        return RemoteLibraryUtil.isBound(mPluginInfo.componentName);
    }

    private void requestLibrary() {
        try {
            mLibraryService = RemoteLibraryUtil.getService(mPluginInfo.componentName);
            if (TextUtils.isEmpty(mLibraryIdentity)) {
                Intent i = new Intent();
                mLibraryService.getLibraryChooserIntent(i);
                startActivityForResult(i, REQUEST_LIBRARY);
            } else {
                addPages();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void addPages() {
        if (mLibraryService != null) {
            try {
                final Bundle b = new Bundle(2);
                b.putParcelable(ARG_COMPONENT, mPluginInfo.componentName);
                b.putString(ARG_IDENTITY, mLibraryIdentity);
                int abilities = mLibraryService.getCapabilities();
                if ((abilities & BROWSE_FOLDERS) == BROWSE_FOLDERS) {
                    mPagerAdapter.add(LibraryFragment.FOLDER, b);
                }
                if ((abilities & QUERY_ARTISTS) == QUERY_ARTISTS) {
                    mPagerAdapter.add(LibraryFragment.ARTIST, b);
                }
                if ((abilities & QUERY_ALBUMS) == QUERY_ALBUMS) {
                    mPagerAdapter.add(LibraryFragment.ALBUM, b);
                }
                mPagerAdapter.notifyDataSetChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
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

        public void add(final LibraryFragment fragment, final Bundle params) {
            mHolderList.add(new FragmentHolder(fragment, params));
        }

        private final static class FragmentHolder {
            LibraryFragment fragment;
            Bundle params;
            private FragmentHolder(LibraryFragment fragment, Bundle params) {
                this.fragment = fragment;
                this.params = params;
            }
        }
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public static enum LibraryFragment {
        /**
         * The playlist fragment
         */
//        PLAYLIST(HomePlaylistFragment.class, R.string.page_playlists),
        /**
         * The recent fragment
         */
//        RECENT(HomeRecentFragment.class, R.string.page_recent),
        /**
         * The artist fragment
         */
        ARTIST(LibraryArtistFragment.class, R.string.page_artists),
        /**
         * The album fragment
         */
        ALBUM(LibraryAlbumFragment.class, R.string.page_albums),
        /**
         * The song fragment
         */
        SONG(LibrarySongFragment.class, R.string.page_songs),
        /**
         * The genre fragment
         */
//        GENRE(HomeGenreFragment.class, R.string.page_genres),

        FOLDER(LibraryFolderFragment.class, R.string.page_folders);

        private Class<? extends Fragment> mFragmentClass;
        private int mTitleResource;

        private LibraryFragment(final Class<? extends Fragment> fragmentClass,
                                final int titleResource) {
            mFragmentClass = fragmentClass;
            mTitleResource = titleResource;
        }

        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

        public int getTitleResource() {
            return mTitleResource;
        }

    }
}
