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

package org.opensilk.music.ui2.library;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;

import java.util.ArrayList;

import rx.Observable;
import timber.log.Timber;

/**
 * Created by drew on 10/20/14.
 */
public class LibraryAdapter2 extends RecyclerView.Adapter<LibraryAdapter2.ViewHolder> {

    final LibraryConnection connection;
    final ArrayList<Bundleable> items;

    public LibraryAdapter2(LibraryConnection connection) {
        this.connection = connection;
        this.items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Bundleable b = getItem(position);
        if (b instanceof Album) {
            bindAlbum(viewHolder, (Album)b);
        } else if (b instanceof Artist) {
            bindArtist(viewHolder, (Artist)b);
        } else if (b instanceof Folder) {
            bindFolder(viewHolder, (Folder)b);
        } else if (b instanceof Song) {
            bindSong(viewHolder, (Song)b);
        } else {
            Timber.e("Some how an invalid Bundleable slipped through.");
        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public Bundleable getItem(int position) {
        return items.get(position);
    }

    void bindAlbum(ViewHolder holder, Album album) {

    }

    void bindArtist(ViewHolder holder, Artist artist) {

    }

    void bindFolder(ViewHolder holder, Folder folder) {

    }

    void bindSong(ViewHolder holder, Song song) {

    }

    LibraryConnection.Result lastResult;

    public void onNewResult(LibraryConnection.Result result) {
        lastResult = result;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
