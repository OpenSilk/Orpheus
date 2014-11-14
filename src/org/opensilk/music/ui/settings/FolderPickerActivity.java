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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.ui2.BaseActivity;
import org.opensilk.silkdagger.DaggerInjector;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 7/13/14.
 */
public class FolderPickerActivity extends BaseActivity {

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
        getSupportActionBar().setTitle(makeTitle(null));
        getSupportActionBar().setSubtitle(makeSubtitle(null));

        setResult(RESULT_CANCELED, getIntent());

        if (savedInstanceState == null) {
            final String action = getIntent().getStringExtra(EXTRA_DIR);
            FolderPickerFragment f = FolderPickerFragment.newInstance(action);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, f, "folders")
                    .commit();
        }

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

    static List<Folder> doListing(File rootDir) {
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] dirList = rootDir.listFiles();
        List<Folder> folders = new ArrayList<>(dirList.length);
        for (File f : dirList) {
            if (f.isDirectory()) {
                folders.add(makeFolder(f));
            }
        }
        Collections.sort(folders, new Comparator<Folder>() {
            @Override
            public int compare(Folder lhs, Folder rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        return folders;
    }

    static Folder makeFolder(File dir) {
        return new Folder.Builder()
                .setIdentity(dir.getAbsolutePath())
                .setName(dir.getName())
                .setChildCount(dir.list().length)
                .setDate(formatDate(dir.lastModified()))
                .build();
    }

    static String formatDate(long ms) {
        Date date = new Date(ms);
        DateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return out.format(date);
    }

    static class ViewHolder {
        final View itemView;
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) TextView extraInfo;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
            ButterKnife.inject(this, itemView);
            itemView.setFocusable(false);
            itemView.setClickable(false);
            overflow.setVisibility(View.GONE);
        }

        public void initImage(String s) {
            LetterTileDrawable drawable = new LetterTileDrawable(itemView.getResources());
            drawable.setText(s);
            artwork.setImageDrawable(drawable);
        }

        public void reset() {
            if (artwork != null) artwork.setImageBitmap(null);
        }
    }

    public static class FolderPickerFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

        public static FolderPickerFragment newInstance(String startDir) {
            FolderPickerFragment f = new FolderPickerFragment();
            Bundle b = new Bundle();
            b.putString(FolderPickerActivity.EXTRA_DIR, startDir);
            f.setArguments(b);
            return f;
        }

        private String mPath;
        private ArrayAdapter<Folder> mAdapter;
        private Subscription mSubscription;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPath = getArguments().getString(FolderPickerActivity.EXTRA_DIR);
            if (TextUtils.isEmpty(mPath)) {
                mPath = FolderPickerActivity.SDCARD_ROOT;
            }

            mAdapter = new ArrayAdapter<Folder>(getActivity(), -1) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    ViewHolder h;
                    if (v == null) {
                        v = LayoutInflater.from(getContext()).inflate(R.layout.gallery_list_item_artwork, parent, false);
                        h = new ViewHolder(v);
                        v.setTag(h);
                    } else {
                        h = (ViewHolder) v.getTag();
                        h.reset();
                    }
                    Folder f = getItem(position);
                    h.title.setText(f.name);
                    h.subtitle.setText(MusicUtils.makeLabel(getContext(), R.plurals.Nitems, f.childCount));
                    h.extraInfo.setText(f.date);
                    h.initImage(f.name);
                    return v;
                }
            };

            mSubscription = Observable.create(new Observable.OnSubscribe<List<Folder>>() {
                @Override
                public void call(Subscriber<? super List<Folder>> subscriber) {
                    List<Folder> l = doListing(new File(mPath));
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onNext(l);
                    subscriber.onCompleted();
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Folder>>() {
                @Override
                public void call(List<Folder> folders) {
                    mAdapter.addAll(folders);
                    setListAdapter(mAdapter);
                }
            });

        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mSubscription.unsubscribe();
            mAdapter = null;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setOnItemLongClickListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            Toast.makeText(getActivity(), R.string.settings_storage_msg_select_help, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            FolderPickerActivity activity = (FolderPickerActivity)getActivity();

            final String path = mAdapter.getItem(position).getIdentity();
            final String title = mAdapter.getItem(position).getName();
            ActionBar actionBar = activity.getSupportActionBar();
            actionBar.setTitle(makeTitle(path));
            actionBar.setSubtitle(makeSubtitle(path));

            FolderPickerFragment f = FolderPickerFragment.newInstance(path);

            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.main, f, title);
            ft.addToBackStack(title);
            ft.commit();
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i = new Intent().putExtra(EXTRA_DIR, mAdapter.getItem(position).getIdentity());
            getActivity().setResult(RESULT_OK, i);
            getActivity().finish();
            return true;
        }
    }

}
