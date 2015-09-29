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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;


import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.rx.SimpleObserver;
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui.widget.GridTileDescription;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 10/20/14.
 */
public class BundleableRecyclerAdapter extends RecyclerListAdapter<Bundleable, BundleableRecyclerAdapter.ViewHolder> {

    final BundleablePresenter presenter;

    boolean gridStyle;
    boolean dragableList;

    final Map<View, SubCont> itemClickSubscriptions = new WeakHashMap<>();
    final Map<View, SubCont> overflowClickSubscriptions = new WeakHashMap<>();

    @Inject
    public BundleableRecyclerAdapter(
            BundleablePresenter presenter
    ) {
        this.presenter = presenter;
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, viewType));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        final Bundleable b = getItem(position);
        if (b instanceof Album) {
            bindAlbum(viewHolder, (Album) b);
        } else if (b instanceof Artist) {
            bindArtist(viewHolder, (Artist)b);
        } else if (b instanceof Folder) {
            bindFolder(viewHolder, (Folder) b);
        } else if (b instanceof Genre) {
            bindGenre(viewHolder, (Genre) b);
        } else if (b instanceof Playlist) {
            bindPlaylist(viewHolder, (Playlist)b);
        } else if (b instanceof Track) {
            bindTrack(viewHolder, (Track) b);
        } else if (b instanceof TrackList) {
            bindTrackCollection(viewHolder, (TrackList) b);
        } else {
            Timber.e("Somehow an invalid Bundleable slipped through.");
        }
        itemClickSubscriptions.put(viewHolder.itemView,
                SubCont.ni(position, RxView.clickEvents(viewHolder.itemView).subscribe(itemClickObserver)));
        overflowClickSubscriptions.put(viewHolder.overflow,
                SubCont.ni(position, RxView.clickEvents(viewHolder.overflow).subscribe(overflowClickObserver)));
    }

    void bindAlbum(ViewHolder holder, Album album) {
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(album.getArtistName(), null, album.getName(), album.getArtworkUri());
        holder.title.setText(album.getName());
        holder.subtitle.setText(album.getArtistName());
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, album.getName());
        } else {
            PaletteObserver paletteObserver = holder.descriptionContainer != null
                    ? holder.descriptionContainer.getPaletteObserver() : null;
            holder.subscriptions.add(presenter.getRequestor().newRequest(holder.artwork,
                    paletteObserver, artInfo, ArtworkType.THUMBNAIL));
        }
    }

    void bindArtist(ViewHolder holder, Artist artist) {
        ArtInfo artInfo = ArtInfo.forArtist(artist.getName(), null);
        holder.title.setText(artist.getName());
        Context context = holder.itemView.getContext();
        String subtitle = "";
        if (artist.getAlbumCount() > 0) {
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nalbums, artist.getAlbumCount());
        }
        if (artist.getTrackCount() > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nsongs, artist.getTrackCount());
        }
        holder.subtitle.setText(subtitle);
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, artist.getName());
        } else {
            PaletteObserver paletteObserver = holder.descriptionContainer != null
                    ? holder.descriptionContainer.getPaletteObserver() : null;
            holder.subscriptions.add(presenter.getRequestor().newRequest(holder.artwork,
                    paletteObserver, artInfo, ArtworkType.THUMBNAIL));
        }
    }

    void bindFolder(ViewHolder holder, Folder folder) {
        holder.title.setText(folder.getName());
        Context context = holder.itemView.getContext();
        if (folder.getChildCount() > 0) {
            holder.subtitle.setText(UtilsCommon.makeLabel(context, R.plurals.Nitems, folder.getChildCount()));
        } else {
            holder.subtitle.setText(" ");
        }
        if (holder.extraInfo != null) {
            holder.extraInfo.setText(folder.getDateModified());
            holder.extraInfo.setVisibility(View.VISIBLE);
        }
        setLetterTileDrawable(holder, folder.getName());
    }

    void bindGenre(ViewHolder holder, Genre genre) {
        holder.title.setText(genre.getName());
        Context context = holder.itemView.getContext();
        String l2 = UtilsCommon.makeLabel(context, R.plurals.Nalbums, genre.getAlbumsCount())
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, genre.getTracksCount());
        holder.subtitle.setText(l2);
        if (gridStyle && genre.getArtInfos().size() > 0) {
            loadMultiArtwork(holder, genre.getArtInfos());
        } else {
            setLetterTileDrawable(holder, genre.getName());
        }
    }

    void bindPlaylist(ViewHolder holder, Playlist playlist) {
        holder.title.setText(playlist.getName());
        Context context = holder.itemView.getContext();
        holder.subtitle.setText(UtilsCommon.makeLabel(context, R.plurals.Nsongs, playlist.getTracksCount()));
        if (gridStyle && (playlist.getArtInfos().size() > 0)) {
            loadMultiArtwork(holder, playlist.getArtInfos());
        } else {
            setLetterTileDrawable(holder, playlist.getName());
        }
    }

    void bindTrack(ViewHolder holder, Track track) {
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(track.getAlbumArtistName(), track.getArtistName(),
                track.getAlbumName(), track.getArtworkUri());
        holder.title.setText(track.getName());
        holder.subtitle.setText(track.getArtistName());
        if (holder.extraInfo != null && track.getDurationS() > 0) {
            holder.extraInfo.setText(UtilsCommon.makeTimeString(holder.itemView.getContext(), track.getDurationS()));
            holder.extraInfo.setVisibility(View.VISIBLE);
        }
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, track.getName());
        } else {
            holder.subscriptions.add(presenter.getRequestor().newRequest(holder.artwork,
                    null, artInfo, ArtworkType.THUMBNAIL));
        }
    }

    void bindTrackCollection(ViewHolder holder, TrackList collection) {
        holder.title.setText(collection.getName());
        Context context = holder.itemView.getContext();
        String l2 = UtilsCommon.makeLabel(context, R.plurals.Nalbums, collection.getAlbumsCount())
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, collection.getTracksCount());
        holder.subtitle.setText(l2);
        if (gridStyle && collection.getArtInfos().size() > 0) {
            loadMultiArtwork(holder, collection.getArtInfos());
        } else {
            setLetterTileDrawable(holder, collection.getName());
        }
    }

    void setLetterTileDrawable(ViewHolder holder, String text) {
        Resources resources = holder.itemView.getResources();
        LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
        drawable.setIsCircular(!gridStyle);
        holder.artwork.setImageDrawable(drawable);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        SubCont c = itemClickSubscriptions.remove(holder.itemView);
        if (c != null) {
            c.sub.unsubscribe();
        }
        c = overflowClickSubscriptions.remove(holder.overflow);
        if (c != null) {
            c.sub.unsubscribe();
        }
        holder.reset();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        itemClickSubscriptions.clear();
        overflowClickSubscriptions.clear();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (dragableList) {
            return R.layout.gallery_list_item_dragsort;
        } else if (!gridStyle) {
            return R.layout.gallery_list_item_artwork;
        } else if (wantsMultiArtwork(position)) {
            return R.layout.gallery_grid_item_artwork4;
        } else {
            return R.layout.gallery_grid_item_artwork;
        }
    }

    public void setGridStyle(boolean gridStyle) {
        this.gridStyle = gridStyle;
    }

    public void setDragableList(boolean dragable) {
        this.dragableList = dragable;
    }

    protected boolean wantsMultiArtwork(int position) {
        Bundleable item = getItem(position);
        if (item instanceof Genre) {
            return ((Genre) item).getArtInfos().size() > 1;
        } else if (item instanceof Playlist) {
            return ((Playlist) item).getArtInfos().size() > 1;
        } else if (item instanceof TrackList) {
            return ((TrackList) item).getArtInfos().size() > 1;
        } else {
            return false;
        }
    }

    void loadMultiArtwork(ViewHolder holder, List<ArtInfo> artInfos) {
        ArtworkRequestManager requestor = presenter.getRequestor();
        CompositeSubscription cs = holder.subscriptions;
        AnimatedImageView artwork = holder.artwork;
        AnimatedImageView artwork2 = holder.artwork2;
        AnimatedImageView artwork3 = holder.artwork3;
        AnimatedImageView artwork4 = holder.artwork4;
        ArtworkType artworkType = ArtworkType.THUMBNAIL;
        UtilsCommon.loadMultiArtwork(requestor, cs, artwork, artwork2, artwork3, artwork4, artInfos, artworkType);
    }

    final Observer<ViewClickEvent> itemClickObserver = new SimpleObserver<ViewClickEvent>() {
        @Override
        public void onNext(ViewClickEvent onClickEvent) {
            SubCont c = itemClickSubscriptions.get(onClickEvent.view());
            if (c != null) {
                Context context = onClickEvent.view().getContext();
                presenter.onItemClicked(context, getItem(c.pos));
            }
        }
    };

    final Observer<ViewClickEvent> overflowClickObserver = new SimpleObserver<ViewClickEvent>() {
        @Override
        public void onNext(ViewClickEvent onClickEvent) {
            SubCont c = overflowClickSubscriptions.get(onClickEvent.view());
            if (c != null) {
                final Context context = onClickEvent.view().getContext();
                final Bundleable bundleable = getItem(c.pos);
                PopupMenu m = new PopupMenu(context, onClickEvent.view());
                presenter.onOverflowClicked(context, m, bundleable);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            OverflowAction action = OverflowAction.valueOf(item.getItemId());
                            return presenter.onOverflowActionClicked(context, action, bundleable);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                });
                m.show();
            }
        }
    };

    public static class SubCont {
        final int pos;
        final Subscription sub;

        public SubCont(int pos, Subscription sub) {
            this.pos = pos;
            this.sub = sub;
        }

        public static SubCont ni(int pos, Subscription sub) {
            return new SubCont(pos, sub);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements DragSwipeViewHolder {
        @InjectView(R.id.artwork_thumb) public AnimatedImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional public AnimatedImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional public AnimatedImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional public AnimatedImageView artwork4;
        @InjectView(R.id.grid_description) @Optional GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) @Optional TextView extraInfo;
        @InjectView(R.id.tile_overflow) ImageButton overflow;
        @InjectView(R.id.drag_handle) @Optional View dragHandle;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            subscriptions.clear();
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            if (descriptionContainer != null) descriptionContainer.resetBackground();
            if (extraInfo != null && extraInfo.getVisibility() != View.GONE) extraInfo.setVisibility(View.GONE);
        }

        @Override
        public void onItemSelected() {

        }

        @Override
        public void onItemClear() {

        }

        @Override
        public View getDragHandle() {
            return dragHandle;
        }
    }

}
