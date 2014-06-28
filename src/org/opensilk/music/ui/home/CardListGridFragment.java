/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andrew.apollo.R;

import hugo.weaving.DebugLog;
import it.gmariotti.cardslib.library.view.CardGridView;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Static library support version of the framework's {@link android.app.ListFragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 *
 * Opensilk: Switched out ListView for CardListView and CardGridView that
 *           can be swapped out.
 */
public abstract class CardListGridFragment extends Fragment {
    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            if (wantGridView()) {
                mGrid.focusableViewAvailable(mGrid);
            } else {
                mList.focusableViewAvailable(mList);
            }
        }
    };

    ListAdapter mAdapter;
    CardListView mList;
    CardGridView mGrid;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mListContainer;
    CharSequence mEmptyText;
    boolean mListShown;

    public CardListGridFragment() {
    }

    public abstract int getListViewLayout();
    public abstract int getGridViewLayout();
    public abstract int getEmptyViewLayout();
    public abstract boolean wantGridView();

    /**
     * Provide default implementation to return a simple list view.  Subclasses
     * can override to replace with their own layout.  If doing so, the
     * returned view hierarchy <em>must</em> have a ListView whose id
     * is {@link android.R.id#list android.R.id.list} and can optionally
     * have a sibling view id {@link android.R.id#empty android.R.id.empty}
     * that is to be shown when the list is empty.
     *
     * <p>If you are overriding this method with your own custom content,
     * consider including the standard layout {@link android.R.layout#list_content}
     * in your layout file, so that you continue to retain all of the standard
     * behavior of ListFragment.  In particular, this is currently the only
     * way to have the built-in indeterminant progress state be shown.
     */
    @Override
    @DebugLog
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);

        //
        // TODO if this can be optimized im all for it
        //

        View tv = inflater.inflate(getEmptyViewLayout(), null);
//        TextView tv = new TextView(getActivity());
        tv.setId(INTERNAL_EMPTY_ID);
//        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        View lv;
        if (wantGridView()) {
            lv = inflater.inflate(getGridViewLayout(), null);
            ((CardGridView) lv).setDrawSelectorOnTop(false);
        } else {
            lv = inflater.inflate(getListViewLayout(), null);
            ((CardListView) lv).setDrawSelectorOnTop(false);
        }
//        CardListView lv = new CardListView(getActivity());
//        lv.setId(android.R.id.list);
//        CardListView lv = (CardListView) inflater.inflate(getListViewLayout(), null);
//        lv.setDrawSelectorOnTop(false);
        lframe.addView(lv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return root;
    }

    /**
     * Attach to list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        mListShown = false;
        mEmptyView = mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    /**
     * Provide the cursor for the list view.
     */
    public void setListAdapter(ListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (wantGridView()) {
            if (mGrid != null) {
                mGrid.setAdapter(adapter);
                if (!mListShown && !hadAdapter) {
                    // The list was hidden, and previously didn't have an
                    // adapter.  It is now time to show it.
                    setListShown(true, getView().getWindowToken() != null);
                }
            }
        } else {
            if (mList != null) {
                mList.setAdapter(adapter);
                if (!mListShown && !hadAdapter) {
                    // The list was hidden, and previously didn't have an
                    // adapter.  It is now time to show it.
                    setListShown(true, getView().getWindowToken() != null);
                }
            }
        }
    }

    /**
     * Set the currently selected list item to the specified
     * position with the adapter's data
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureList();
        if (wantGridView()) {
            mGrid.setSelection(position);
        } else {
            mList.setSelection(position);
        }
    }

    /**
     * Get the position of the currently selected list item.
     */
    public int getSelectedItemPosition() {
        ensureList();
        if (wantGridView()) {
            return mGrid.getSelectedItemPosition();
        } else {
            return mList.getSelectedItemPosition();
        }
    }

    /**
     * Get the cursor row ID of the currently selected list item.
     */
    public long getSelectedItemId() {
        ensureList();
        if (wantGridView()) {
            return mGrid.getSelectedItemId();
        } else {
            return mList.getSelectedItemId();
        }
    }

    /**
     * Get the activity's list view widget.
     */
    public CardListView getListView() {
        ensureList();
        if (wantGridView()) {
            throw new NullPointerException("Can't get list when grid was requested");
        }
        return mList;
    }

    public CardGridView getGridView() {
        ensureList();
        if (!wantGridView()) {
            throw new NullPointerException("Can't get grid when list was requested");
        }
        return mGrid;
    }

    /**
     * The default content for a ListFragment has a TextView that can
     * be shown when the list is empty.  If you would like to have it
     * shown, call this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text) {
        ensureList();
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        mStandardEmptyView.setText(text);
        if (mEmptyText == null) {
            if (wantGridView()) {
                mGrid.setEmptyView(mEmptyView);
            } else {
                mList.setEmptyView(mEmptyView);
            }
        }
        mEmptyText = text;
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * <p>Applications do not normally need to use this themselves.  The default
     * behavior of ListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with {@link #setListAdapter(ListAdapter)}.
     * If the list at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     *
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    /**
     * Like {@link #setListShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Get the ListAdapter associated with this activity's ListView.
     */
    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    @DebugLog
    private void ensureList() {
        if (wantGridView()) {
            if (mGrid != null) return;
        } else {
            if (mList != null) return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (wantGridView()) {
            if (root instanceof CardGridView) {
                mGrid = (CardGridView)root;
            } else {
                View ev = root.findViewById(INTERNAL_EMPTY_ID);
                if (ev instanceof TextView) {
                    mStandardEmptyView = (TextView)ev;
                    mEmptyView = mStandardEmptyView;
                } else {
                    mEmptyView = ev;
                    View et = root.findViewById(android.R.id.empty);
                    if (et instanceof TextView) {
                        mStandardEmptyView = (TextView)et;
                    } else {
                        throw new RuntimeException("Your empty view must be either a TextView or " +
                                "contain a TextView with id android.R.id.empty");
                    }
                }
                mEmptyView.setVisibility(View.GONE);
                mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
                mListContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);
                View rawListView = root.findViewById(android.R.id.list);
                if (!(rawListView instanceof CardGridView)) {
                    if (rawListView == null) {
                        throw new RuntimeException(
                                "Your content must have a CardGridView whose id attribute is " +
                                        "'android.R.id.list'");
                    }
                    throw new RuntimeException(
                            "Content has view with id attribute 'android.R.id.list' "
                                    + "that is not a CardGridView class");
                }
                mGrid = (CardGridView)rawListView;
                if (mEmptyView != null) {
                    mGrid.setEmptyView(mEmptyView);
                } else if (mEmptyText != null) {
                    mStandardEmptyView.setText(mEmptyText);
                    mGrid.setEmptyView(mStandardEmptyView);
                }
            }
        } else {
            if (root instanceof CardListView) {
                mList = (CardListView)root;
            } else {
                View ev = root.findViewById(INTERNAL_EMPTY_ID);
                if (ev instanceof TextView) {
                    mStandardEmptyView = (TextView)ev;
                    mEmptyView = mStandardEmptyView;
                } else {
                    mEmptyView = ev;
                    View et = root.findViewById(android.R.id.empty);
                    if (et instanceof TextView) {
                        mStandardEmptyView = (TextView)et;
                    } else {
                        throw new RuntimeException("Your empty view must be either a TextView or " +
                                "contain a TextView with id android.R.id.empty");
                    }
                }
                mEmptyView.setVisibility(View.GONE);
//                mStandardEmptyView = (TextView)root.findViewById(INTERNAL_EMPTY_ID);
//                if (mStandardEmptyView == null) {
//                    mEmptyView = root.findViewById(android.R.id.empty);
//                } else {
//                    mStandardEmptyView.setVisibility(View.GONE);
//                }
                mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
                mListContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);
                View rawListView = root.findViewById(android.R.id.list);
                if (!(rawListView instanceof CardListView)) {
                    if (rawListView == null) {
                        throw new RuntimeException(
                                "Your content must have a ListView whose id attribute is " +
                                        "'android.R.id.list'");
                    }
                    throw new RuntimeException(
                            "Content has view with id attribute 'android.R.id.list' "
                                    + "that is not a ListView class");
                }
                mList = (CardListView)rawListView;
                if (mEmptyView != null) {
                    mList.setEmptyView(mEmptyView);
                } else if (mEmptyText != null) {
                    mStandardEmptyView.setText(mEmptyText);
                    mList.setEmptyView(mStandardEmptyView);
                }
            }
        }
        mListShown = true;
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }
}
