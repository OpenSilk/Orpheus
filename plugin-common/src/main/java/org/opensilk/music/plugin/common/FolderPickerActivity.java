/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.plugin.common;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.spi.Bundleable;

/**
 * Created by drew on 7/18/14.
 */
public class FolderPickerActivity extends Activity implements ServiceConnection {

    public static final String SERVICE_COMPONENT = "service_component";
    public static final String STARTING_FOLDER = "starting_folder";
    public static final String PICKED_FOLDER_IDENTITY = "picked_folder_identity";
    public static final String PICKED_FOLDER_TITLE = "picked_folder_title";

    private RemoteLibrary mLibrary;
    private String mSourceIdentity;
    private ComponentName mServiceComponent;
    private String mStartingFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean wantLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppThemeDark);
        }

        FrameLayout root = new FrameLayout(this);
        root.setId(android.R.id.content);
        setContentView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mSourceIdentity = getIntent().getStringExtra(OrpheusApi.EXTRA_LIBRARY_ID);
        mServiceComponent = getIntent().getParcelableExtra(SERVICE_COMPONENT);
        mStartingFolder = getIntent().getStringExtra(STARTING_FOLDER);

        bindService(new Intent().setComponent(mServiceComponent), this, BIND_AUTO_CREATE);

        // because i dont want to fuck with configuration changes
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        setResult(RESULT_CANCELED, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mLibrary = RemoteLibrary.Stub.asInterface(service);
        pushFolder(mSourceIdentity, mStartingFolder);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mLibrary = null;
    }

    private void pushFolder(String libraryid, String folderid) {
        FragmentTransaction ft = getFragmentManager().beginTransaction()
                .replace(android.R.id.content, PickerFragment.newInstance(libraryid, folderid));
        if (!TextUtils.isEmpty(folderid) && !TextUtils.equals(folderid, mStartingFolder)) {
            ft.addToBackStack(folderid);
        }
        ft.commit();
    }

    private void onFolderSelected(String identity, String title) {
        setResult(RESULT_OK, getIntent().putExtra(PICKED_FOLDER_IDENTITY, identity)
                .putExtra(PICKED_FOLDER_TITLE, title));
        finish();
    }

    public static class PickerFragment extends ListFragment implements
            AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

        private FolderPickerActivity mActivity;
        private LibraryArrayAdapter mAdapter;
        private LibraryEndlessAdapter mEndlessAdapter;
        private String mSourceIdentity;
        private String mFolderIdentity;

        public static PickerFragment newInstance(String identity, String folderId) {
            PickerFragment f = new PickerFragment();
            Bundle b = new Bundle();
            b.putString("__id", identity);
            b.putString("f__id", folderId);
            f.setArguments(b);
            return f;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (FolderPickerActivity) activity;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSourceIdentity = getArguments().getString("__id");
            mFolderIdentity = getArguments().getString("f__id");
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mAdapter = new LibraryArrayAdapter(getActivity(), mActivity.mLibrary,
                    new LibraryInfo(mSourceIdentity, null, mFolderIdentity, null));
            mEndlessAdapter = new LibraryEndlessAdapter(getActivity(), mAdapter);
            setListAdapter(mEndlessAdapter);
            getListView().setOnItemClickListener(this);
            getListView().setOnItemLongClickListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            Toast.makeText(mActivity, R.string.toast_how_to_pick, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Bundleable item = (Bundleable) mEndlessAdapter.getItem(position);
            if ((item instanceof Folder)
                    || (item instanceof Album)
                    || (item instanceof Artist)) {
                mActivity.pushFolder(mSourceIdentity, item.getIdentity());
            } else {
                Toast.makeText(mActivity, R.string.err_song_click, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Bundleable item = (Bundleable) mEndlessAdapter.getItem(position);
            if ((item instanceof Folder)
                    || (item instanceof Album)
                    || (item instanceof Artist)) {
                mActivity.onFolderSelected(item.getIdentity(), item.getName());
                return true;
            } else {
                Toast.makeText(mActivity, R.string.err_song_click, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
