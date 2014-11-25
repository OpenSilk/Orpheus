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

package org.opensilk.music.ui2.search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.model.Playlist;

import org.opensilk.common.content.RecyclerListAdapter;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.ui2.profile.AlbumScreen;
import org.opensilk.music.widgets.GridTileDescription;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.Mortar;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 11/24/14.
 */
public class SearchAdapter extends RecyclerListAdapter<Object, SearchAdapter.ViewHolder> {

    final LayoutInflater inflater;
    final Context context;

    public SearchAdapter(Context context) {
        super();
        this.context = context;
        this.inflater = LayoutInflater.from(context);
//        Mortar.inject(context, this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(inflater.inflate(i, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, int i) {
        Object o = getItem(i);
        if (o instanceof ListHeader) {
            ((TextView)vh.itemView).setText(((ListHeader)o).title);
        } else if (o instanceof LocalAlbum) {
            bindLocalAlbum(vh, (LocalAlbum)o);
        } else if (o instanceof LocalArtist) {
            bindLocalArtist(vh, (LocalArtist)o);
        } else if (o instanceof Genre) {
            bindGenre(vh, (Genre)o);
        } else if (o instanceof Playlist) {
            bindPlaylist(vh, (Playlist)o);
        } else if (o instanceof LocalSong) {
            bindLocalSong(vh, (LocalSong)o);
        } else if (o instanceof BundleableHolder) {
            Bundleable o2 = ((BundleableHolder)o).bundleable;
            if (o2 instanceof Album) {
                bindAlbum(vh, (Album)o2);
            } else if (o2 instanceof Artist) {
                bindArtist(vh, (Artist)o2);
            } else if (o2 instanceof Folder) {
                bindFolder(vh, (Folder)o2);
            } else if (o2 instanceof Song) {
                bindSong(vh, (Song)o2);
            }
        }
    }

    protected void bindLocalAlbum(ViewHolder vh, final LocalAlbum album) {
        vh.title.setText(album.name);
        vh.subtitle.setText(album.artistName);
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new AlbumScreen(album));
            }
        });
    }

    protected void bindLocalArtist(ViewHolder vh, final LocalArtist artist) {
        vh.title.setText(artist.name);
        vh.subtitle.setText(null);
    }

    protected void bindGenre(ViewHolder vh, final Genre genre) {
        vh.title.setText(genre.mGenreName);
        vh.subtitle.setText(null);
    }

    protected void bindPlaylist(ViewHolder vh, final Playlist p) {
        vh.title.setText(p.mPlaylistName);
        vh.subtitle.setText(null);
    }

    protected void bindLocalSong(ViewHolder vh, final LocalSong s) {
        vh.title.setText(s.name);
        vh.subtitle.setText(s.artistName);
    }

    protected void bindAlbum(ViewHolder vh, final Album album) {
        vh.title.setText(album.name);
        vh.subtitle.setText(album.artistName);
    }

    protected void bindArtist(ViewHolder vh, final Artist artist) {
        vh.title.setText(artist.name);
        vh.subtitle.setText(null);
    }

    protected void bindFolder(ViewHolder vh, final Folder folder) {
        vh.title.setText(folder.name);
        vh.subtitle.setText(null);
    }

    protected void bindSong(ViewHolder vh, final Song song) {
        vh.title.setText(song.name);
        vh.subtitle.setText(song.artistName);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset();
    }

    @Override
    public int getItemViewType(int i) {
        Object o = getItem(i);
        if (o instanceof ListHeader) {
            return R.layout.search_tile_header;
        }
        return R.layout.gallery_list_item_artwork;
    }

    public static class ListHeader {
        public final String title;
        public ListHeader(String title) {
            this.title = title;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
            subscriptions = new CompositeSubscription();
            if (itemView instanceof TextView) {
                return;
            }
            ButterKnife.inject(this, itemView);
            overflow.setVisibility(View.GONE);
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            subscriptions.clear();
        }

    }
}
