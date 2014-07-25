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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.R;
import org.opensilk.music.api.meta.LibraryInfo;

import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.cards.event.AlbumCardClick;
import org.opensilk.music.ui.cards.event.ArtistCardClick;
import org.opensilk.music.ui.cards.event.FolderCardClick;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.util.PluginSettings;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryFragment extends ScopedDaggerFragment implements BackButtonListener, RemoteLibraryHelper.ConnectionListener {

    public static final int REQUEST_LIBRARY = 1001;
    public static final int REQUEST_SETTINGS = 1002;
    public static final String ARG_LIBRARY_INFO = "argLibraryInfo";

    @Inject @ForActivity
    protected ActionBarController mActionBarHelper;
    @Inject @ForActivity
    protected DrawerHelper mDrawerHelper;
    @Inject @ForFragment
    protected Bus mFragmentBus;
    @Inject @ForFragment
    protected RemoteLibraryHelper mLibrary;

    protected PluginSettings mSettings;

    private PluginInfo mPluginInfo;
    private String mLibraryIdentity;

    private FragmentBusMonitor mFragmentMonitor;
    private SongCardClickHandler mSongClickHandler;

    private boolean mFromSavedInstance;
    private int mCapabilities;

    public static LibraryFragment newInstance(PluginInfo p) {
        LibraryFragment f = new LibraryFragment();
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
        mLibrary.acquireService(mPluginInfo.componentName, this);
        // settings
        mSettings = new PluginSettings(getActivity(), mPluginInfo.componentName);
        // register with bus
        mFragmentMonitor = new FragmentBusMonitor();
        mSongClickHandler = getObjectGraph().get(SongCardClickHandler.class);
        mFragmentBus.register(mFragmentMonitor);
        mFragmentBus.register(mSongClickHandler);
        // restore state
        if (savedInstanceState != null) {
            mFromSavedInstance = true;
            mLibraryIdentity = savedInstanceState.getString("library_id");
        } else {
            mLibraryIdentity = mSettings.getDefaultSource();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.blank_framelayout_topmargin, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set title
        mActionBarHelper.setTitle(mPluginInfo.title);
        // enable overflow
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            RemoteLibrary l = mLibrary.getService();
            if (l != null) {
                l.resume();
            }
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            RemoteLibrary l = mLibrary.getService();
            if (l != null) {
                l.pause();
            }
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onDestroy() {
        mFragmentBus.unregister(mFragmentMonitor);
        mFragmentBus.unregister(mSongClickHandler);
        mLibrary.releaseService();
        super.onDestroy();
    }

    @Override
    //@DebugLog
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LIBRARY:
                if (resultCode == Activity.RESULT_OK) {
                    final String id = data.getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
                    if (TextUtils.isEmpty(id)) {
                        throw new RuntimeException("Library chooser must set EXTRA_LIBRARY_ID");
                    }
                    mLibraryIdentity = id;
                    mSettings.setDefaultSource(mLibraryIdentity);
                    initFolderFragment();
                } else {
                    //TODO
                }
                break;
            case REQUEST_SETTINGS:
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("library_id", mLibraryIdentity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mDrawerHelper.isDrawerOpen()) {
            // set title (hacks)
            mActionBarHelper.setTitle(mPluginInfo.title);
            // search
            if ((mCapabilities & OrpheusApi.Ability.SEARCH) != 0) {
                inflater.inflate(R.menu.search, menu);
            }

            Resources res= null;
            try {
                res = getActivity().getPackageManager()
                        .getResourcesForApplication(mPluginInfo.componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {}

            // device selection
            inflater.inflate(R.menu.change_source, menu);
            if (res != null) {
                try {
                    String change = res.getString(res.getIdentifier("menu_change_source",
                            "string", mPluginInfo.componentName.getPackageName()));
                    menu.findItem(R.id.menu_change_source).setTitle(change);
                } catch (Resources.NotFoundException ignored) {}
            }

            // settings
            if ((mCapabilities & OrpheusApi.Ability.SETTINGS) != 0) {
                inflater.inflate(R.menu.library_settings, menu);
                if (res != null) {
                    try {
                        String settings = res.getString(res.getIdentifier("menu_library_settings",
                                "string", mPluginInfo.componentName.getPackageName()));
                        menu.findItem(R.id.menu_library_settings).setTitle(settings);
                    } catch (Resources.NotFoundException ignored) {}
                }
            }


        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                openSearchFragment();
                return true;
            case R.id.menu_change_source:
                relaunchLibraryFragment();
                return true;
            case R.id.menu_library_settings:
                openLibrarySettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Abstract Methods
     */

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new LibraryModule(),
        };
    }

    @Override
    protected DaggerInjector getParentInjector(Activity activity) {
        return (DaggerInjector) activity;
    }

    /*
     * BackButtonListener
     */

    @Override
    //@DebugLog
    public boolean onBackButtonPressed() {
        return getChildFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onConnected() {
        if (!mFromSavedInstance) {
            try {
                resolveCapabilities();
                if (TextUtils.isEmpty(mLibraryIdentity)) {
                    Intent i = new Intent();
                    mLibrary.getService().getLibraryChooserIntent(i);
                    if (i.getComponent() != null) {
                        i.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, ThemeHelper.isLightTheme(getActivity()));
                        startActivityForResult(i, REQUEST_LIBRARY);
                    }
                } else {
                    initFolderFragment();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionBroke() {
        Timber.d("onConnectionBroke");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLibrary.acquireService(mPluginInfo.componentName, LibraryFragment.this);
            }
        }, 3000);
    }

    private void initFolderFragment() {
        final LibraryInfo li = new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, null);
        Fragment f = FolderFragment.newInstance(li);
        FragmentManager fm = getChildFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fm.beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }

    private void pushFolderFragment(String folderId) {
        final LibraryInfo li = new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, folderId);
        Fragment f = FolderFragment.newInstance(li);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.container, f)
                .addToBackStack(folderId)
                .commit();
    }

    public void openSearchFragment() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.container,SearchFragment.newInstance(
                        new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, null)))
                .addToBackStack("search")
                .commit();
    }

    public void relaunchLibraryFragment() {
        mSettings.clearDefaultSource();
        NavUtils.openLibrary(getActivity(), mPluginInfo);
    }

    private void openLibrarySettings() {
        try {
            Intent i = new Intent();
            mLibrary.getService().getSettingsIntent(i);
            if (i.getComponent() != null) {
                i.putExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, ThemeHelper.isLightTheme(getActivity()));
                i.putExtra(OrpheusApi.EXTRA_LIBRARY_ID, mLibraryIdentity);
                startActivityForResult(i, REQUEST_SETTINGS);
            }
        } catch (RemoteException|NullPointerException e) {
            //TODO
        }
    }

    private void resolveCapabilities() {
        try {
            mCapabilities = mLibrary.getService().getCapabilities();
            getActivity().invalidateOptionsMenu();
        } catch (RemoteException|NullPointerException e) {
            e.printStackTrace();
        }
    }

    class FragmentBusMonitor {
        @Subscribe
        public void onFolderClicked(FolderCardClick e) {
            final LibraryInfo li = new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, e.folder.identity);
            switch (e.event) {
                case OPEN:
                    pushFolderFragment(e.folder.identity);
                    break;
                case PLAY_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.PLAY_ALL);
                    break;
                case SHUFFLE_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.SHUFFLE_ALL);
                    break;
                case ADD_TO_QUEUE:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.ADD_QUEUE);
                    break;
            }
        }

        @Subscribe
        public void onArtistCardClicked(ArtistCardClick e) {
            final LibraryInfo li = new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, e.artist.identity);
            switch (e.event) {
                case OPEN:
                    pushFolderFragment(e.artist.identity);
                    break;
                case PLAY_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.PLAY_ALL);
                    break;
                case SHUFFLE_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.SHUFFLE_ALL);
                    break;
                case ADD_TO_QUEUE:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.ADD_QUEUE);
                    break;
            }
        }

        @Subscribe
        public void onAlbumCardClicked(AlbumCardClick e) {
            final LibraryInfo li = new LibraryInfo(mLibraryIdentity, mPluginInfo.componentName, e.album.identity);
            switch (e.event) {
                case OPEN:
                    pushFolderFragment(e.album.identity);
                    break;
                case PLAY_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.PLAY_ALL);
                    break;
                case SHUFFLE_ALL:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.SHUFFLE_ALL);
                    break;
                case ADD_TO_QUEUE:
                    doBackgroundWork(li, BackgroundFetcherFragment.Action.ADD_QUEUE);
                    break;
            }
        }

        protected void doBackgroundWork(LibraryInfo info, BackgroundFetcherFragment.Action action) {
            FetchingProgressFragment.newInstance(info, action)
                    .show(getActivity().getSupportFragmentManager(), FetchingProgressFragment.FRAGMENT_TAG);
        }

    }

}
