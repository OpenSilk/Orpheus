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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.widgets.ColorCodedThumbnail;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 10/20/14.
 */
public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    final LibraryScreen.Presenter presenter;
    final ArrayList<Bundleable> items;

    LayoutInflater inflater;

    public LibraryAdapter(LibraryScreen.Presenter presenter) {
        this.presenter = presenter;
        this.items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (inflater == null) {
            inflater = LayoutInflater.from(viewGroup.getContext());
        }
        View v = inflater.inflate(viewType, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (!endofResults && position == getItemCount()-1) {
            if (lastResult != null && lastResult.token != null) {
                presenter.loadMore(lastResult.token);
            }
        }
        final Bundleable b = getItem(position);
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
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onItemClicked(v.getContext(), b);
            }
        });
        viewHolder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.gallery_list_item_artwork;
    }

    public Bundleable getItem(int position) {
        return items.get(position);
    }

    void bindAlbum(ViewHolder holder, Album album) {
        ArtInfo artInfo = new ArtInfo(album.artistName, album.name, album.artworkUri);
        holder.title.setText(album.name);
        holder.subtitle.setText(album.artistName);
        holder.subscriptions.add(presenter.requestor.newAlbumRequest((AnimatedImageView) holder.artwork,
                null, artInfo, ArtworkType.THUMBNAIL));
    }

    void bindArtist(ViewHolder holder, Artist artist) {
        ArtInfo artInfo = new ArtInfo(artist.name, null, null);
        holder.title.setText(artist.name);
        String subtitle = MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nalbums, artist.albumCount)
                + ", " + MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, artist.songCount);
        holder.subtitle.setText(subtitle);
        holder.subscriptions.add(presenter.requestor.newArtistRequest((AnimatedImageView) holder.artwork,
                null, artInfo, ArtworkType.THUMBNAIL));
    }

    void bindFolder(ViewHolder holder, Folder folder) {
        holder.title.setText(folder.name);
        if (folder.childCount > 0) {
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nitems, folder.childCount));
        } else {
            holder.subtitle.setText(" ");
        }
        holder.extraInfo.setText(folder.date);
        holder.extraInfo.setVisibility(View.VISIBLE);
        LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
        drawable.setText(folder.name);
        holder.artwork.setImageDrawable(drawable);
    }

    void bindSong(ViewHolder holder, Song song) {
        ArtInfo artInfo = new ArtInfo(song.artistName, song.albumArtistName, song.artworkUri);
        holder.title.setText(song.name);
        holder.subtitle.setText(song.artistName);
        if (song.duration > 0) {
            holder.extraInfo.setText(MusicUtils.makeTimeString(holder.itemView.getContext(), song.duration));
            holder.extraInfo.setVisibility(View.VISIBLE);
        }
        holder.subscriptions.add(presenter.requestor.newAlbumRequest((AnimatedImageView) holder.artwork,
                null, artInfo, ArtworkType.THUMBNAIL));
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
            int oldpos = getItemCount();
            items.addAll(result.items);
            notifyItemRangeInserted(oldpos, result.items.size());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) TextView extraInfo;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        public void reset() {
            if (artwork != null) artwork.setImageBitmap(null);
            if (extraInfo.getVisibility() != View.GONE) extraInfo.setVisibility(View.GONE);
            subscriptions.clear();
        }
    }
}
