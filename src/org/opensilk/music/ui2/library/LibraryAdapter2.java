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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.w3c.dom.Text;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import timber.log.Timber;

/**
 * Created by drew on 10/20/14.
 */
public class LibraryAdapter2 extends RecyclerView.Adapter<LibraryAdapter2.ViewHolder> {

    final LibraryScreen.Presenter presenter;
    final ArrayList<Bundleable> items;

    LayoutInflater inflater;

    public LibraryAdapter2(LibraryScreen.Presenter presenter) {
        this.presenter = presenter;
        this.items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        if (inflater == null) {
            inflater = LayoutInflater.from(viewGroup.getContext());
        }
        View v = inflater.inflate(getItemViewType(position), viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (!endofResults && position == getItemCount()-1) {
            if (lastResult != null && lastResult.token != null) {
                presenter.loadMore(lastResult.token);
            }
        }
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
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return android.R.layout.simple_list_item_2;
    }

    public Bundleable getItem(int position) {
        return items.get(position);
    }

    void bindAlbum(ViewHolder holder, Album album) {
        holder.title.setText(album.name);
    }

    void bindArtist(ViewHolder holder, Artist artist) {
        holder.title.setText(artist.name);
    }

    void bindFolder(ViewHolder holder, Folder folder) {
        holder.title.setText(folder.name);
    }

    void bindSong(ViewHolder holder, Song song) {
        holder.title.setText(song.name);
    }

    LibraryConnection.Result lastResult;
    boolean endofResults;

    public void onNewResult(LibraryConnection.Result result) {
        if (result.token == null) {
            endofResults = true;
        }
        if (result.items.isEmpty()) {
            endofResults = true;
            return;
        }
        lastResult = result;
        if (getItemCount() == 0) {
            items.addAll(result.items);
            notifyDataSetChanged();
        } else {
            int oldpos = getItemCount()-1;
            items.addAll(result.items);
            notifyItemRangeInserted(oldpos, getItemCount());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(android.R.id.text1) TextView title;
        @InjectView(android.R.id.text2) TextView subTitle;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
