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

package org.opensilk.music.ui.folder;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;

import org.opensilk.common.widget.CustomListGridFragment;
import org.opensilk.music.R;

import org.opensilk.filebrowser.FileBrowserArgs;
import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.FileItemArrayLoader;
import org.opensilk.music.ui.cards.FolderPickerCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * Created by drew on 7/13/14.
 */
public class FolderPickerFragment extends CustomListGridFragment implements LoaderManager.LoaderCallbacks<List<FileItem>> {

    public static FolderPickerFragment newInstance(String startDir) {
        FolderPickerFragment f = new FolderPickerFragment();
        Bundle b = new Bundle();
        b.putString(FolderPickerActivity.EXTRA_DIR, startDir);
        f.setArguments(b);
        return f;
    }

    private CardArrayAdapter mAdapter;
    private FileBrowserArgs args;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = new FileBrowserArgs();
        String path = getArguments().getString(FolderPickerActivity.EXTRA_DIR);
        if (TextUtils.isEmpty(path)) {
            path = FolderPickerActivity.SDCARD_ROOT;
        }
        args.setPath(path);
        Set<Integer> mediaTypes = new HashSet<Integer>();
        mediaTypes.add(FileItem.MediaType.DIRECTORY);
        args.setMediaTypes(mediaTypes);

        mAdapter = new CardArrayAdapter(getActivity(), new ArrayList<Card>());

        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText("No folders");
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        if (mAdapter.isEmpty()) {
            setListShown(false);
        }
    }

    @Override
    public int getListViewLayout() {
        return R.layout.card_listview;
    }

    @Override
    public int getEmptyViewLayout() {
        return R.layout.list_empty_view;
    }

    @Override
    public Loader<List<FileItem>> onCreateLoader(int id, Bundle args) {
        return new FileItemArrayLoader(getActivity(), this.args);
    }

    @Override
    public void onLoadFinished(Loader<List<FileItem>> loader, List<FileItem> data) {
        if (data == null || data.size() == 0) {
            mAdapter.clear();
            return;
        }
        if (!mAdapter.isEmpty() && isViewCreated()) {
            setListShown(false);
        }
        mAdapter.clear();
        List<Card> cards = new ArrayList<>(data.size());
        for (FileItem item : data) {
            cards.add(new FolderPickerCard(getActivity(), item));
        }
        mAdapter.addAll(cards);
        if (isViewCreated()) {
            setListShown(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<FileItem>> loader) {
        mAdapter.clear();
    }
}
