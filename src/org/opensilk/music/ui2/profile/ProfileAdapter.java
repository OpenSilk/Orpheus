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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.content.RecyclerListAdapter;
import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.widgets.GridTileDescription;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.Mortar;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 11/21/14.
 */
public class ProfileAdapter extends RecyclerListAdapter<Object, ProfileAdapter.ViewHolder> {

    @Inject OverflowHandlers.LocalAlbums albumOverflowHandler;
    @Inject OverflowHandlers.LocalSongGroups songGroupOverflowHandler;
    @Inject OverflowHandlers.LocalSongs songOverflowHandler;
    @Inject ArtworkRequestManager requestor;

    final LayoutInflater inflater;
    final Context context;

    boolean useSimpleLayout = false;
    boolean isPlaylist = false;
    boolean isLastAddedPlaylist = false;

    public ProfileAdapter(Context context) {
        super();
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        Mortar.inject(context, this);
    }

    public ProfileAdapter(Context context, boolean useSimpleLayout) {
        this(context);
        this.useSimpleLayout = useSimpleLayout;
    }

    public ProfileAdapter(Context context, boolean isPlaylist, boolean isLastAddedPlaylist) {
        this(context);
        this.isPlaylist = isPlaylist;
        this.isLastAddedPlaylist = isLastAddedPlaylist;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return (i == -1) ? new ViewHolder((View) getItem(0))
                : new ViewHolder(inflater.inflate(i, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, int i) {
        Object obj = getItem(i);
        if (obj instanceof LocalAlbum) {
            final LocalAlbum la = (LocalAlbum)obj;
            vh.title.setText(la.name);
            vh.subtitle.setText(la.artistName);
            PaletteObserver paletteObserver = vh.descriptionContainer != null
                    ? vh.descriptionContainer.getPaletteObserver() : null;
            vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork,
                    paletteObserver, new ArtInfo(la.artistName, la.name, la.artworkUri), ArtworkType.THUMBNAIL));
            vh.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(context, v);
                    albumOverflowHandler.populateMenu(m, la);
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                return albumOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), la);
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                    });
                    m.show();
                }
            });
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppFlow.get(context).goTo(new AlbumScreen(la));
                }
            });
        } else if (obj instanceof LocalSongGroup) {
            final LocalSongGroup lsg = (LocalSongGroup) obj;
            vh.title.setText(lsg.name);
            String l2 = MusicUtils.makeLabel(context, R.plurals.Nalbums, lsg.albumIds.length)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, lsg.songIds.length);
            vh.subtitle.setText(l2);
            loadMultiArtwork(
                    requestor,
                    vh.subscriptions,
                    lsg.albumIds,
                    vh.artwork,
                    vh.artwork2,
                    vh.artwork3,
                    vh.artwork4
            );
            vh.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(context, v);
                    songGroupOverflowHandler.populateMenu(m, lsg);
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                return songGroupOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), lsg);
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                    });
                    m.show();
                }
            });
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppFlow.get(context).goTo(new SongGroupScreen(lsg));
                }
            });
        } else if (obj instanceof LocalSong) {
            final LocalSong song = (LocalSong) obj;
            vh.title.setText(song.name);
            vh.subtitle.setText(song.artistName);
            vh.info.setVisibility(View.VISIBLE);
            vh.info.setText(MusicUtils.makeTimeString(context, song.duration));
            if (!useSimpleLayout && vh.artwork != null) {
                vh.subscriptions.add(requestor.newAlbumRequest(vh.artwork, null, song.albumId, ArtworkType.THUMBNAIL));
            }
            vh.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(context, v);
                    songOverflowHandler.populateMenu(m, song);
                    if (isPlaylist) {
                        m.getMenu().removeItem(R.id.popup_delete);
                    }
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                return songOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), song);
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                    });
                    m.show();
                }
            });
            vh.clicker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = 0; int count = getItemCount();
                    LocalSong[] songs = new LocalSong[count];
                    for (int ii=0; ii<count; ii++) {
                        songs[ii] = (LocalSong) getItem(ii);
                        if (songs[ii].songId == song.songId) {
                            pos = ii;
                        }
                    }
                    songOverflowHandler.playAll(songs, pos);
                }
            });
        } else if (obj instanceof View) {
            //header
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset();
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getItem(position);
        if (o instanceof View) {
            return -1;
        } else if (o instanceof LocalAlbum) {
            return R.layout.gallery_grid_item_artwork;
        } else if (o instanceof LocalSongGroup) {
            LocalSongGroup lsg = (LocalSongGroup)o;
            if (lsg.albumIds.length >= 2) {
                return R.layout.gallery_grid_item_artwork4;
            } else {
                return R.layout.gallery_grid_item_artwork;
            }
        } else if (o instanceof LocalSong) {
            if (isPlaylist && !isLastAddedPlaylist) {
                return R.layout.gallery_list_item_dragsort;
            } else if (useSimpleLayout) {
                return R.layout.gallery_list_item_simple;
            } else {
                return R.layout.gallery_list_item_artwork;
            }
        }
        throw new IllegalArgumentException("Unknown class " + o.getClass());
    }

    public void addHeader(View header) {
        addItem(0, header);
    }

    static CompositeSubscription loadMultiArtwork(ArtworkRequestManager requestor,
                                                  CompositeSubscription cs,
                                                  long[] albumIds,
                                                  AnimatedImageView artwork,
                                                  AnimatedImageView artwork2,
                                                  AnimatedImageView artwork3,
                                                  AnimatedImageView artwork4) {
        final int num = albumIds.length;
        if (artwork != null) {
            if (num >= 1) {
                cs.add(requestor.newAlbumRequest(artwork, null, albumIds[0], ArtworkType.THUMBNAIL));
            } else {
                artwork.setDefaultImage();
            }
        }
        if (artwork2 != null) {
            if (num >= 2) {
                cs.add(requestor.newAlbumRequest(artwork2, null, albumIds[1], ArtworkType.THUMBNAIL));
            } else {
                // never get here
                artwork2.setDefaultImage();
            }
        }
        if (artwork3 != null) {
            if (num >= 3) {
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[2], ArtworkType.THUMBNAIL));
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[1], ArtworkType.THUMBNAIL));
            } else {
                // never get here
                artwork3.setDefaultImage();
            }
        }
        if (artwork4 != null) {
            if (num >= 4) {
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[3], ArtworkType.THUMBNAIL));
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[0], ArtworkType.THUMBNAIL));
            } else {
                //never get here
                artwork4.setDefaultImage();
            }
        }
        return cs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) @Optional AnimatedImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional AnimatedImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional AnimatedImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional AnimatedImageView artwork4;
        @InjectView(R.id.grid_description) @Optional GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) @Optional TextView title;
        @InjectView(R.id.tile_subtitle) @Optional TextView subtitle;
        @InjectView(R.id.tile_info) @Optional TextView info;
        @InjectView(R.id.tile_overflow) @Optional ImageButton overflow;
        @InjectView(R.id.tile_content) @Optional View clicker;

        final CompositeSubscription subscriptions;
        final int artNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
            if (artwork4 != null) {
                artNumber = 4;
            } else if (artwork2 != null) {
                artNumber = 2;
            } else {
                artNumber = 1;
            }
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            if (descriptionContainer != null) descriptionContainer.resetBackground();
            if (info != null && info.getVisibility() != View.GONE) info.setVisibility(View.GONE);
            subscriptions.clear();
        }
    }

}
