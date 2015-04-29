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
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.proj.FolderTrackProj;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.FolderTrackSortOrder;
import org.opensilk.music.library.util.CursorUtil;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 7/18/14.
 */
public class FolderPickerActivity extends Activity {

    public static final String STARTING_FOLDER = "starting_folder";
    public static final String PICKED_FOLDER_IDENTITY = "picked_folder_identity";
    public static final String PICKED_FOLDER_TITLE = "picked_folder_title";

    private String mAuthority;
    private String mLibraryId;
    private String mStartingFolder;

    public static Intent buildIntent(Intent parent, Context context,
                                     String authority, String libraryId, String startFolder) {
        return new Intent(context, FolderPickerActivity.class)
                .putExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, parent.getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, false))
                .putExtra(LibraryConstants.EXTRA_LIBRARY_AUTHORITY, authority)
                .putExtra(LibraryConstants.EXTRA_LIBRARY_ID, libraryId)
                .putExtra(FolderPickerActivity.STARTING_FOLDER, startFolder);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean wantLightTheme = getIntent().getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppThemeDark);
        }

        FrameLayout root = new FrameLayout(this);
        root.setId(android.R.id.content);
        setContentView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mAuthority = getIntent().getStringExtra(LibraryConstants.EXTRA_LIBRARY_AUTHORITY);
        mLibraryId = getIntent().getStringExtra(LibraryConstants.EXTRA_LIBRARY_ID);
        mStartingFolder = getIntent().getStringExtra(STARTING_FOLDER);

        setResult(RESULT_CANCELED, null);

        if (savedInstanceState == null) {
            pushFolder(mAuthority, mLibraryId, mStartingFolder);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void pushFolder(String authority, String libraryid, String folderid) {
        FragmentTransaction ft = getFragmentManager().beginTransaction()
                .replace(android.R.id.content, PickerFragment.newInstance(authority, libraryid, folderid));
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
            AdapterView.OnItemClickListener,
            AdapterView.OnItemLongClickListener,
            LoaderManager.LoaderCallbacks<Cursor> {

        private FolderPickerActivity mActivity;
        private String mAuthority;
        private String mSourceIdentity;
        private String mFolderIdentity;
        private ArrayAdapter<Bundleable> mAdapter;

        public static PickerFragment newInstance(String authority, String identity, String folderId) {
            PickerFragment f = new PickerFragment();
            Bundle b = new Bundle();
            b.putString("__a", authority);
            b.putString("__id", identity);
            b.putString("__fid", folderId);
            f.setArguments(b);
            return f;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (FolderPickerActivity) activity;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mActivity = null;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAuthority = getArguments().getString("__a");
            mSourceIdentity = getArguments().getString("__id");
            mFolderIdentity = getArguments().getString("__fid");
            mAdapter = new ArrayAdapter<Bundleable>(getActivity(), android.R.layout.simple_list_item_1);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            setListAdapter(null);
            mAdapter = null;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setEmptyText(getString(R.string.no_results));
            getListView().setOnItemClickListener(this);
            getListView().setOnItemLongClickListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            Toast.makeText(mActivity, R.string.toast_how_to_pick, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Bundleable item = (Bundleable) mAdapter.getItem(position);
            if ((item instanceof Folder)
                    || (item instanceof Album)
                    || (item instanceof Artist)) {
                mActivity.pushFolder(mAuthority, mSourceIdentity, item.getIdentity());
            } else {
                Toast.makeText(mActivity, R.string.err_song_click, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Bundleable item = (Bundleable) mAdapter.getItem(position);
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

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(
                    mActivity,
                    LibraryUris.folder(mAuthority, mSourceIdentity, mFolderIdentity),
                    FolderTrackProj.ALL,
                    null,
                    null,
                    FolderTrackSortOrder.A_Z
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                mAdapter.clear();
                data.moveToFirst();
                while (!data.isAfterLast()) {
                    mAdapter.add(CursorUtil.fromFolderTrackCursor(data));
                    data.moveToNext();
                }
                setListAdapter(mAdapter);
            } else {
                setListShown(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }
}
