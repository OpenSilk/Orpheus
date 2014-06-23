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

import org.opensilk.music.ui.library.adapter.LibraryFolderGridArrayAdapter;
import org.opensilk.music.ui.library.adapter.LibraryFolderListArrayAdapter;
import org.opensilk.music.ui.library.adapter.LibraryLoaderCallback;
import org.opensilk.music.ui.library.event.FolderCardClick;
import org.opensilk.music.ui.library.module.DirectoryStack;
import org.opensilk.silkdagger.DaggerInjector;
import org.opensilk.silkdagger.qualifier.ForFragment;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/23/14.
 */
public class LibraryFolderGridFragment extends CardGridFragment implements
        LibraryLoaderCallback,
        DirectoryStack {

    private ComponentName mLibraryComponentName;
    private String mLibraryIdentity;

    protected LibraryFolderGridArrayAdapter mAdapter;

    @Inject
    @ForFragment
    Bus mBus;

    /**
     * LIFO stack
     */
    private Deque<Bundle> mDirectoryStack = new ArrayDeque<>();

    public static LibraryFolderGridFragment newInstance(String libraryIdentity, ComponentName libraryComponentName) {
        LibraryFolderGridFragment f = new LibraryFolderGridFragment();
        Bundle b = new Bundle(2);
        b.putString(LibraryHomeFragment.ARG_IDENTITY, libraryIdentity);
        b.putParcelable(LibraryHomeFragment.ARG_COMPONENT, libraryComponentName);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) getParentFragment()).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new RuntimeException("Null args");
        }
        mLibraryComponentName = getArguments().getParcelable(LibraryHomeFragment.ARG_COMPONENT);
        mLibraryIdentity = getArguments().getString(LibraryHomeFragment.ARG_IDENTITY);

        mAdapter = new LibraryFolderGridArrayAdapter(getActivity(), mLibraryIdentity, mLibraryComponentName, this, (DaggerInjector)getParentFragment());
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

        mBus.register(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        if (mAdapter.isOnFirstLoad()) {
            setListShown(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBus.unregister(this);
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
