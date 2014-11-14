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

package org.opensilk.music.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import org.opensilk.common.widget.CustomListGridFragment;
import org.opensilk.filebrowser.FileBrowserArgs;
import org.opensilk.filebrowser.FileItemArrayLoader;
import org.opensilk.music.R;

import org.opensilk.filebrowser.FileItem;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.ui.cards.FolderPickerCard;
import org.opensilk.music.ui2.BaseActivity;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * Created by drew on 7/13/14.
 */
public class FolderPickerActivity extends BaseActivity implements Card.OnCardClickListener, Card.OnLongCardClickListener {

    @dagger.Module(includes = BaseActivity.Module.class, injects = FolderPickerActivity.class)
    public static class Module {
    }

    public static final String EXTRA_DIR = "start_dir";
    public static final String SDCARD_ROOT;
    static {
        SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean light = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        setTheme(light ? R.style.Theme_Settings_Light : R.style.Theme_Settings_Dark);

        ((DaggerInjector) getApplication()).getObjectGraph().plus(new Module()).inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.blank_framelayout_toolbar);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED, getIntent());

        if (savedInstanceState == null) {
            final String action = getIntent().getStringExtra(EXTRA_DIR);
            FolderPickerFragment f = FolderPickerFragment.newInstance(action);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, f, "folders")
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(Card card, View view) {
        boolean addToBackstack = true;
        final FolderPickerCard c = (FolderPickerCard) card;
        final String path = c.getData().getPath();
        final String title = c.getData().getTitle();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(makeTitle(path));
        actionBar.setSubtitle(makeSubtitle(path));
        if (c.getData().getMediaType() == FileItem.MediaType.UP_DIRECTORY) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                return;
            } else {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                addToBackstack = false;
            }
        }
        FolderPickerFragment f = FolderPickerFragment.newInstance(path);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main, f, title);
        if (addToBackstack) {
            ft.addToBackStack(title);
        }
        ft.commit();
    }

    @Override
    public boolean onLongClick(Card card, View view) {
        Intent i = new Intent().putExtra(EXTRA_DIR, ((FolderPickerCard) card).getData().getPath());
        setResult(RESULT_OK, i);
        finish();
        return true;
    }

    protected static String makeSubtitle(String path) {
        if (TextUtils.isEmpty(path)) {
            path = SDCARD_ROOT;
        }
        if (path.startsWith(SDCARD_ROOT)) {
            path = path.replace(SDCARD_ROOT, "SDCARD");
        }
        return path;
    }

    protected static String makeTitle(String path) {
        if (TextUtils.isEmpty(path)) {
            path = SDCARD_ROOT;
        }
        if (TextUtils.equals(path, SDCARD_ROOT)) {
            return "SDCARD";
        } else if (path.contains("/") && !path.endsWith("/")) {
            return path.substring(path.lastIndexOf("/")+1);
        } else {
            return path;
        }
    }

    public static class FolderPickerFragment extends CustomListGridFragment implements LoaderManager.LoaderCallbacks<List<FileItem>> {

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

}
