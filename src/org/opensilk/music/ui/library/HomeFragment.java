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
import android.os.Bundle;
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
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.Api;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.RemoteLibraryEvent;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.cards.event.FolderCardClick;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.BackButtonListener;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.music.util.RemoteLibraryUtil;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForActivity;
import org.opensilk.silkdagger.qualifier.ForFragment;
import org.opensilk.silkdagger.support.ScopedDaggerFragment;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/14/14.
 */
public class HomeFragment extends ScopedDaggerFragment implements BackButtonListener {

    public static final int REQUEST_LIBRARY = 1001;
    public static final String ARG_COMPONENT = "argComponent";
    public static final String ARG_IDENTITY = "argIdentity";
    public static final String ARG_FOLDER_ID = "argFolderId";

    @Inject @ForActivity
    ActionBarController mActionBarHelper;
    @Inject @ForActivity
    DrawerHelper mDrawerHelper;
    @Inject @ForFragment
    Bus mFragmentBus;

    private RemoteLibrary mLibraryService;
    private PluginInfo mPluginInfo;
    private String mLibraryIdentity;

    private GlobalBusMonitor mGlobalMonitor;
    private FragmentBusMonitor mFragmentMonitor;

    private boolean mWantGridView;
    private boolean mFromSavedInstance;

    public static HomeFragment newInstance(PluginInfo p) {
        HomeFragment f = new HomeFragment();
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
        // register with busses
        mGlobalMonitor = new GlobalBusMonitor();
        EventBus.getInstance().register(mGlobalMonitor);
        mFragmentMonitor = new FragmentBusMonitor();
        mFragmentBus.register(mFragmentMonitor);
        // restore state
        if (savedInstanceState != null) {
            mFromSavedInstance = true;
            mLibraryIdentity = savedInstanceState.getString("library_id");
            mWantGridView = savedInstanceState.getBoolean("want_grid");
        } else {
            // TODO save / get from prefs
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.topmargin_container, container, false);
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
    public void onDestroy() {
        mFragmentBus.unregister(mFragmentMonitor);
        EventBus.getInstance().unregister(mGlobalMonitor);
        RemoteLibraryUtil.unbindFromService(getActivity(), mPluginInfo.componentName);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mDrawerHelper.isDrawerOpen()) {
            inflater.inflate(R.menu.view_as, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_as_simple:
                mWantGridView = false;
                initFolderFragment();
                return true;
            case R.id.menu_view_as_grid:
                mWantGridView = true;
                initFolderFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    initFolderFragment();
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
        outState.putBoolean("want_grid", mWantGridView);
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
    @DebugLog
    public boolean onBackButtonPressed() {
        return getChildFragmentManager().popBackStackImmediate();
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
            } else if (!mFromSavedInstance) {
                initFolderFragment();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void initFolderFragment() {
        Fragment f;
        if (mWantGridView) {
            f = FolderGridFragment.newInstance(mLibraryIdentity, mPluginInfo.componentName, null);
        } else {
            f = FolderListFragment.newInstance(mLibraryIdentity, mPluginInfo.componentName, null);
        }
        FragmentManager fm = getChildFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        fm.beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }

    private void pushFolderFragment(String folderId) {
        Fragment f;
        if (mWantGridView) {
            f = FolderGridFragment.newInstance(mLibraryIdentity, mPluginInfo.componentName, folderId);
        } else {
            f = FolderListFragment.newInstance(mLibraryIdentity, mPluginInfo.componentName, folderId);
        }
        getChildFragmentManager().beginTransaction()
                .replace(R.id.container, f)
                .addToBackStack(folderId)
                .commit();
    }

    class GlobalBusMonitor {
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
    }

    class FragmentBusMonitor {
        @Subscribe
        public void onFolderClicked(FolderCardClick e) {
            switch (e.event) {
                case OPEN:
                    pushFolderFragment(e.folder.identity);
                    break;
                case PLAY_ALL:
                    break;
                case SHUFFLE_ALL:
                    break;
                case ADD_TO_QUEUE:
                    FetchingProgressFragment.newInstance(mLibraryIdentity, mPluginInfo.componentName, e.folder.identity)
                            .show(getChildFragmentManager(), "progress");
                    break;
            }
        }

        @Subscribe
        public void onSongCardEvent(SongCardClick e) {
            switch (e.event) {
                case PLAY:
                    MusicUtils.playAllSongs(getActivity(), new Song[]{e.song}, 0, false);
                    break;
                case PLAY_NEXT:
                    MusicUtils.playNext(getActivity(), new Song[]{e.song});
                    break;
                case ADD_TO_QUEUE:
                    MusicUtils.addSongsToQueue(getActivity(), new Song[]{e.song});
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
                            DeleteDialog.newInstance(e.song.name, new long[]{id}, null)
                                    .show(getChildFragmentManager(), "DeleteDialog");
                        } catch (NumberFormatException ex) {
                            //TODO
                        }
                    } // else unsupported
                    break;
            }
        }
    }

}
