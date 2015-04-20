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

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;

import com.andrew.apollo.model.LocalSong;
import com.mobeta.android.dslv.DragSortListView;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 11/19/14.
 */
public class PlaylistDragSortView extends DragSortListView implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener{

    @Inject PlaylistScreenPresenterDslv presenter;

    final PlaylistAdapter mAdapter;

    public PlaylistDragSortView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        mAdapter = new PlaylistAdapter(getContext(), presenter.playlist.mPlaylistId);
        setDividerHeight(0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (presenter.isLastAdded()) {
            // last added arent sortable
            setDragEnabled(false);
        } else {
            // Set the drop listener
            setDropListener(this);
            // Set the swipe to remove listener
            setRemoveListener(this);
        }
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.takeView(this);
    }

    @Override
    public void remove(final int which) {
        if (presenter.isLastAdded()) return;
        LocalSong song = mAdapter.getItem(which);
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", presenter.playlist.mPlaylistId);
        getContext().getContentResolver().delete(uri,
                MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?",
                new String[]{song.identity});
        mAdapter.remove(song);
    }

    @Override
    public void drop(final int from, final int to) {
        if (presenter.isLastAdded()) return;
        MediaStore.Audio.Playlists.Members.moveItem(getContext().getContentResolver(),
                presenter.playlist.mPlaylistId, from, to);
        LocalSong song = mAdapter.getItem(from);
        mAdapter.remove(song);
        mAdapter.insert(song, to);
    }

    //Separated to ensure header is added first
    void setupAdapter() {
        setAdapter(mAdapter);
    }
}
