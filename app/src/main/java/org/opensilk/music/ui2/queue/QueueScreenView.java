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

package org.opensilk.music.ui2.queue;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import com.andrew.apollo.model.RecentSong;
import com.mobeta.android.dslv.DragSortListView;

import javax.inject.Inject;

import mortar.Mortar;
import timber.log.Timber;

/**
 * Created by drew on 10/15/14.
 */
public class QueueScreenView extends DragSortListView implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener,
        AdapterView.OnItemClickListener {

    @Inject QueueScreenPresenter presenter;

    final QueueScreenAdapter adapter;

    public QueueScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        adapter = new QueueScreenAdapter(getContext(), presenter);
    }

    @Override
    protected void onFinishInflate() {
        Timber.v("onFinishInflate()");
        super.onFinishInflate();
        setAdapter(adapter);
        setDropListener(this);
        setRemoveListener(this);
        setOnItemClickListener(this);
        setDividerHeight(0);
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        Timber.v("onAttachedToWindow()");
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        Timber.v("onDetachedFromWindow()");
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    /*
     * RemoveListener
     */

    @Override
    public void remove(final int which) {
        RecentSong s = adapter.getItem(which);
        adapter.remove(s);
        presenter.removeQueueItem(s.recentId);
    }

    /*
     * Droplistener
     */

    @Override
    public void drop(final int from, final int to) {
        RecentSong s = adapter.getItem(from);
//        adapter.setNotifyOnChange(false);
        adapter.remove(s);
        adapter.insert(s, to);
//        adapter.setNotifyOnChange(true);
//        adapter.notifyDataSetChanged();
        presenter.moveQueueItem(from, to);
    }

    /*
     * ItemClickListener
     */

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // When selecting a track from the queue, just jump there instead of
        // reloading the queue. This is both faster, and prevents accidentally
        // dropping out of party shuffle.
        Timber.d("onItemClick(%d)", position);
        presenter.setQueuePosition(position);
    }

    public void onCurrentSongChanged(long recentId) {
        adapter.currentSong = recentId;
        adapter.notifyDataSetChanged();
    }

    public void onPlaystateChanged(boolean isPlaying) {
        adapter.isPlaying = isPlaying;
        adapter.notifyDataSetChanged();
    }

}
