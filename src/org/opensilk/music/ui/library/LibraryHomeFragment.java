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
import android.util.SparseArray;
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
import org.opensilk.music.ui.library.module.DirectoryStack;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.util.RemoteLibraryUtil;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.support.DaggerFragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static org.opensilk.music.api.Api.Ability.BROWSE_FOLDERS;
import static org.opensilk.music.api.Api.Ability.QUERY_ALBUMS;
import static org.opensilk.music.api.Api.Ability.QUERY_ARTISTS;
import static org.opensilk.music.api.Api.Ability.QUERY_SONGS;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryHomeFragment extends DaggerFragment implements BackButtonListener {

    public static final int REQUEST_LIBRARY = 1001;
    public static final String ARG_COMPONENT = "argComponent";
    public static final String ARG_IDENTITY = "argIdentity";

    @Inject @ForActivity
    ActionBarController mActionBarHelper;

    private RemoteLibrary mLibraryService;
    private PluginInfo mPluginInfo;

    @InjectView(R.id.pager)
    ViewPager mPager;
    protected HomePagerAdapter mPagerAdapter;

    private String mLibraryIdentity;
    private int mPreviousPagerPage = 0;

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
        // Bind the remote service
        RemoteLibraryUtil.bindToService(getActivity(), mPluginInfo.componentName);
        EventBus.getInstance().register(this);
        // init adapter
        mPagerAdapter = new HomePagerAdapter(getActivity(), getChildFragmentManager());
        // restore state
        if (savedInstanceState != null) {
            mLibraryIdentity = savedInstanceState.getString("library_id");
            mPreviousPagerPage = savedInstanceState.getInt("library_pager_current");
        } else {
            // TODO save / get from prefs
        }
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
        mPager.setAdapter(mPagerAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set title
        mActionBarHelper.setTitle(mPluginInfo.title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getInstance().unregister(this);
        RemoteLibraryUtil.unbindFromService(getActivity(), mPluginInfo.componentName);
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
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("library_id", mLibraryIdentity);
        outState.putInt("library_pager_current", mPager.getCurrentItem());
    }

    /*
     * BackButtonListener
     */

    @Override
    @DebugLog
    public boolean onBackButtonPressed() {
        Fragment f = mPagerAdapter.getFragment(mPager.getCurrentItem());
        if (f != null && (f instanceof DirectoryStack)) {
            DirectoryStack d = (DirectoryStack) f;
            return d.popDirectoryStack();
        }
        return false;
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
                int caps = mLibraryService.getCapabilities();

                final Bundle b = new Bundle(2);
                b.putParcelable(ARG_COMPONENT, mPluginInfo.componentName);
                b.putString(ARG_IDENTITY, mLibraryIdentity);

                if ((caps & BROWSE_FOLDERS) == BROWSE_FOLDERS) {
                    mPagerAdapter.add(LibraryFragment.FOLDER, b);
                }
                if ((caps & QUERY_ARTISTS) == QUERY_ARTISTS) {
                    mPagerAdapter.add(LibraryFragment.ARTIST, b);
                }
                if ((caps & QUERY_ALBUMS) == QUERY_ALBUMS) {
                    mPagerAdapter.add(LibraryFragment.ALBUM, b);
                }
                if ((caps & QUERY_SONGS) == QUERY_SONGS) {
                    mPagerAdapter.add(LibraryFragment.SONG, b);
                }

                mPagerAdapter.notifyDataSetChanged();

                mPager.setCurrentItem(mPreviousPagerPage);
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
        private final SparseArray<WeakReference<Fragment>> mFragments = new SparseArray<>();

        public HomePagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, new WeakReference<Fragment>(f));
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            if (mFragments.get(position) != null) {
                mFragments.remove(position);
            }
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

        public Fragment getFragment(int position) {
            WeakReference<Fragment> wf = mFragments.get(position);
            if (wf != null) {
                return wf.get();
            }
            return null;
        }

        public Class<? extends Fragment> getClassAt(int position) {
            return mHolderList.get(position).fragment.getFragmentClass();
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
