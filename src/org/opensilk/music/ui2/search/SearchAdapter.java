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
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.content.RecyclerListAdapter;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.R;
import org.opensilk.music.api.PluginConfig;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.api.model.spi.Bundleable;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.library.LibraryScreen;
import org.opensilk.music.ui2.profile.AlbumScreen;
import org.opensilk.music.ui2.profile.ArtistScreen;
import org.opensilk.music.ui2.profile.GenreScreen;
import org.opensilk.music.ui2.profile.PlaylistScreen;
import org.opensilk.music.widgets.GridTileDescription;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import hugo.weaving.DebugLog;
import mortar.Mortar;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 11/24/14.
 */
public class SearchAdapter extends RecyclerListAdapter<Object, SearchAdapter.ViewHolder> {

    @Inject ArtworkRequestManager requestor;
    @Inject MusicServiceConnection musicService;

    final LayoutInflater inflater;
    final Context context;

    public SearchAdapter(Context context) {
        super();
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        Mortar.inject(context, this);
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
            bindBundleable(vh, (BundleableHolder)o);
        }
    }

    protected void bindLocalAlbum(ViewHolder vh, final LocalAlbum a) {
        vh.title.setText(a.name);
        vh.subtitle.setText(a.artistName);
        vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork,
                null, new ArtInfo(a.artistName, a.name, a.artworkUri), ArtworkType.THUMBNAIL));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new AlbumScreen(a));
            }
        });
    }

    protected void bindLocalArtist(ViewHolder vh, final LocalArtist a) {
        vh.title.setText(a.name);
        vh.subtitle.setText(
                MusicUtils.makeLabel(context, R.plurals.Nalbums, a.albumCount)
                        + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, a.songCount)
        );
        vh.subscriptions.add(requestor.newArtistRequest(vh.artwork,
                null, new ArtInfo(a.name, null, null), ArtworkType.THUMBNAIL));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new ArtistScreen(a));
            }
        });
    }

    protected void bindGenre(ViewHolder vh, final Genre g) {
        vh.title.setText(g.mGenreName);
        vh.subtitle.setText(
                MusicUtils.makeLabel(context, R.plurals.Nalbums, g.mAlbumNumber)
                        + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, g.mSongNumber)
        );
        vh.artwork.setImageDrawable(LetterTileDrawable.fromText(context.getResources(), g.mGenreName));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new GenreScreen(g));
            }
        });
    }

    protected void bindPlaylist(ViewHolder vh, final Playlist p) {
        vh.title.setText(p.mPlaylistName);
        vh.subtitle.setText(MusicUtils.makeLabel(context, R.plurals.Nsongs, p.mSongNumber));
        vh.artwork.setImageDrawable(LetterTileDrawable.fromText(context.getResources(), p.mPlaylistName));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new PlaylistScreen(p));
            }
        });
    }

    protected void bindLocalSong(ViewHolder vh, final LocalSong s) {
        vh.title.setText(s.name);
        vh.subtitle.setText(s.artistName);
        if (s.duration > 0) {
            vh.extraInfo.setText(MusicUtils.makeTimeString(context, s.duration));
            vh.extraInfo.setVisibility(View.VISIBLE);
        }
        vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork,
                null, s.albumId, ArtworkType.THUMBNAIL));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.enqueueNext(new Func0<Song[]>() {
                    @Override
                    public Song[] call() {
                        return new Song[] {s};
                    }
                });
            }
        });
    }

    protected void bindBundleable(ViewHolder vh, final BundleableHolder bh) {
        Bundleable o2 = bh.bundleable;
        if (o2 instanceof Album) {
            bindAlbum(vh, (Album)o2);
        } else if (o2 instanceof Artist) {
            bindArtist(vh, (Artist)o2);
        } else if (o2 instanceof Folder) {
            bindFolder(vh, (Folder)o2);
        } else if (o2 instanceof Song) {
            bindSong(vh, (Song)o2);
            return;
        }
        final PluginInfo pluginInfo = bh.pluginHolder.pluginInfo;
        final PluginConfig pluginConfig = bh.pluginHolder.pluginConfig;
        final LibraryInfo libraryInfo = bh.pluginHolder.libraryInfo.buildUpon(o2.getIdentity(), o2.getName());
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppFlow.get(context).goTo(new LibraryScreen(pluginInfo, pluginConfig, libraryInfo));
            }
        });
    }

    protected void bindAlbum(ViewHolder vh, final Album a) {
        vh.title.setText(a.name);
        vh.subtitle.setText(a.artistName);
        vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork,
                null, makeBestfitArtInfo(a.artistName, null, a.name, a.artworkUri), ArtworkType.THUMBNAIL));
    }

    protected void bindArtist(ViewHolder vh, final Artist a) {
        vh.title.setText(a.name);
        String subtitle = "";
        if (a.albumCount > 0) {
            subtitle += MusicUtils.makeLabel(context, R.plurals.Nalbums, a.albumCount);
        }
        if (a.songCount > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += MusicUtils.makeLabel(context, R.plurals.Nsongs, a.songCount);
        }
        vh.subtitle.setText(subtitle);
        vh.subscriptions.add(requestor.newArtistRequest(vh.artwork,
                null, new ArtInfo(a.name, null, null), ArtworkType.THUMBNAIL));
    }

    protected void bindFolder(ViewHolder vh, final Folder f) {
        vh.title.setText(f.name);
        vh.subtitle.setText(
                f.childCount > 0 ? MusicUtils.makeLabel(context, R.plurals.Nitems, f.childCount) : " "
        );
        if (!TextUtils.isEmpty(f.date)) {
            vh.extraInfo.setText(f.date);
            vh.extraInfo.setVisibility(View.VISIBLE);
        }
        vh.artwork.setImageDrawable(LetterTileDrawable.fromText(context.getResources(), f.name));
    }

    protected void bindSong(ViewHolder vh, final Song s) {
        vh.title.setText(s.name);
        vh.subtitle.setText(s.artistName);
        if (s.duration > 0) {
            vh.extraInfo.setText(MusicUtils.makeTimeString(context, s.duration));
            vh.extraInfo.setVisibility(View.VISIBLE);
        }
        vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork,
                null, makeBestfitArtInfo(s.albumArtistName, s.artistName, s.albumName, s.artworkUri), ArtworkType.THUMBNAIL));
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.enqueueNext(new Func0<Song[]>() {
                    @Override
                    public Song[] call() {
                        return new Song[] {s};
                    }
                });
            }
        });
    }

    static ArtInfo makeBestfitArtInfo(String artist, String altArtist, String album, Uri uri) {
        if (uri != null) {
            if (artist == null || album == null) {
                // we need both to make a query but we have uri so just use that,
                // note this will prevent cache from returning artist images when album is null
                return new ArtInfo(null, null, uri);
            } else {
                return new ArtInfo(artist, album, uri);
            }
        } else {
            if (artist == null && altArtist != null) {
                // cant fallback to uri so best guess the artist
                // note this is a problem because the song artist may not be the
                // album artist but we have no choice here, also note the service
                // does the same thing so at least it will be consistent
                return new ArtInfo(altArtist, album, null);
            } else {
                // if everything is null the artworkmanager will set the default image
                // so no further validation is needed here.
                return new ArtInfo(artist, album, null);
            }
        }
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
        @InjectView(R.id.tile_info) TextView extraInfo;
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
            if (extraInfo != null
                    && extraInfo.getVisibility() != View.GONE)
                extraInfo.setVisibility(View.GONE);
            subscriptions.clear();
        }

    }
}
