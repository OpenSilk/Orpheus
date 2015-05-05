/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.BundleableSortOrder;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.spi.Bundleable;

import java.util.List;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

/**
 * Created by drew on 4/29/15.
 */
public class FolderPickerFragment extends ListFragment implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private FolderPickerActivity mActivity;
    private String mAuthority;
    private LibraryInfo mLibraryInfo;
    private ArrayAdapter<Bundleable> mAdapter;
    private BundleableLoader mLoader;
    private Subscription mLoaderSubscription;

    public static FolderPickerFragment newInstance(String authority, LibraryInfo libraryInfo) {
        FolderPickerFragment f = new FolderPickerFragment();
        Bundle b = new Bundle();
        b.putString("__a", authority);
        b.putParcelable(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
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
        mLibraryInfo = getArguments().getParcelable(LibraryConstants.EXTRA_LIBRARY_INFO);
        mAdapter = new ArrayAdapter<Bundleable>(getActivity(), android.R.layout.simple_list_item_1);
        mLoader = new BundleableLoader(
                getActivity().getApplicationContext(),
                LibraryUris.folderFolders(mAuthority, mLibraryInfo.libraryId, mLibraryInfo.folderId),
                BundleableSortOrder.A_Z
        );
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
        mLoaderSubscription = AndroidObservable.bindFragment(this, mLoader.getListObservable())
                .subscribe(new Action1<List<Bundleable>>() {
                    @Override
                    public void call(List<Bundleable> bundleables) {
                        if (!isResumed()) return;
                        mAdapter.clear();
                        if (!bundleables.isEmpty()) {
                            mAdapter.addAll(bundleables);
                            setListAdapter(mAdapter);
                        } else {
                            setListAdapter(null);
                            setListShown(false);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (!isResumed()) return;
                        mAdapter.clear();
                        setListAdapter(null);
                        setListShown(false);
                        Toast.makeText(getActivity(), R.string.err_loading, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLoaderSubscription != null) {
            mLoaderSubscription.unsubscribe();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundleable item = (Bundleable) mAdapter.getItem(position);
        if ((item instanceof Folder)
                || (item instanceof Album)
                || (item instanceof Artist)
                || (item instanceof Genre)
                || (item instanceof Playlist)) {
            mActivity.pushFolder(mAuthority, mLibraryInfo.buildUpon(item.getIdentity(), item.getName()));
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
            mActivity.onFolderSelected(mLibraryInfo.buildUpon(item.getIdentity(), item.getName()));
            return true;
        } else {
            Toast.makeText(mActivity, R.string.err_song_click, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

}
