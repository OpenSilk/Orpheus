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
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.ui.library.adapter.AbsLibraryArrayAdapter;
import org.opensilk.music.ui.library.adapter.LibraryFolderArrayAdapter;
import org.opensilk.music.ui.library.event.FolderCardClick;
import org.opensilk.music.ui.library.module.DirectoryStack;
import org.opensilk.silkdagger.IDaggerActivity;
import org.opensilk.silkdagger.qualifier.ForActivity;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryFolderFragment extends CardListFragment implements
        AbsLibraryArrayAdapter.LoaderCallback,
        DirectoryStack {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;

    protected LibraryFolderArrayAdapter mAdapter;

    @Inject @ForActivity
    Bus mActivityBus;

    /**
     * LIFO stack
     */
    private Deque<Bundle> mDirectoryStack = new ArrayDeque<>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((IDaggerActivity) activity).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new RuntimeException("Null args");
        }
        mLibraryComponentName = getArguments().getParcelable(LibraryHomeFragment.ARG_COMPONENT);
        mLibraryIdentity = getArguments().getString(LibraryHomeFragment.ARG_IDENTITY);

        mAdapter = new LibraryFolderArrayAdapter(getActivity(), mLibraryIdentity, mLibraryComponentName, this);
        if (savedInstanceState != null) {
            mAdapter.restoreInstanceState(savedInstanceState);
            Parcelable[] bundles = savedInstanceState.getParcelableArray("dirstack");
            if (bundles != null) {
                for (Parcelable p : bundles) {
                    // toArray gives us our stack reversed
                    mDirectoryStack.addLast((Bundle) p);
                }
            }
        } else {
            mAdapter.startLoad(null);
        }

        mActivityBus.register(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        if (savedInstanceState == null) {
            setListShown(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivityBus.unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
        if (mDirectoryStack.size() > 0) {
            Bundle[] bundles = mDirectoryStack.toArray(new Bundle[mDirectoryStack.size()]);
            outState.putParcelableArray("dirstack", bundles);
        }
    }

    /*
     * AbsLibraryArrayAdapter.LoaderCallback
     */

    @Override
    @DebugLog
    public void onFirstLoadComplete() {
        setListShown(true);
    }

    /*
     * DirectoryStack
     */

    @Override
    @DebugLog
    public boolean popDirectoryStack() {
        if (mDirectoryStack.peekFirst() != null) {
            Bundle b = mDirectoryStack.removeFirst();
            mAdapter.restoreInstanceState(b);
            return true;
        }
        return false;
    }

    /*
     * Events
     */

    @Subscribe
    public void onFolderClicked(FolderCardClick e) {
        Bundle b = new Bundle();
        mAdapter.saveInstanceState(b);
        mDirectoryStack.addFirst(b);
        setListShown(false);
        mAdapter.startLoad(e.folderId);
    }

}
