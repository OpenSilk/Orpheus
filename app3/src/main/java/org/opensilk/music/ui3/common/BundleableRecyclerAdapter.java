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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.glide.PaletteSwatchType;
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.ui.widget.GridTileDescription;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 10/20/14.
 */
public class BundleableRecyclerAdapter extends RecyclerListAdapter<Bundleable, BundleableRecyclerAdapter.ViewHolder>
        implements ItemClickSupport.OnItemClickListener, ItemClickSupport.OnItemLongClickListener {

    public interface OnSelectionModeEnded {
        void onEndSelectionMode();
    }

    final BundleablePresenter presenter;
    //todo clear when dataset changes
    final SparseBooleanArray selectedItems = new SparseBooleanArray();
    private boolean inSelectionMode;
    private final OnSelectionModeEnded selectionEndedListener = new OnSelectionModeEnded() {
        @Override
        public void onEndSelectionMode() {
            for (int ii=0; ii<getItemCount(); ii++) {
                if (selectedItems.get(ii)) {
                    selectedItems.put(ii, false);
                    notifyItemChanged(ii);
                }
            }
            selectedItems.clear();
            inSelectionMode = false;
        }
    };

    boolean gridStyle;
    boolean dragableList;
    boolean lightTheme;

    @Inject
    public BundleableRecyclerAdapter(
            BundleablePresenter presenter
    ) {
        this.presenter = presenter;
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(this)
                .setOnItemLongClickListener(this);
        lightTheme = ThemeUtils.isLightTheme(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.removeFrom(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, viewType));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.reset();
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
        if (inSelectionMode && selectedItems.get(position)) {
            viewHolder.onItemSelected();
        }
    }

    void bindAlbum(ViewHolder holder, Album album) {
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(album.getArtistName(), null, album.getName(), album.getArtworkUri());
        holder.title.setText(album.getName());
        holder.subtitle.setText(album.getArtistName());
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, album.getName());
        } else {
            loadArtwork(artInfo, holder);
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
            loadArtwork(artInfo, holder);
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
        if (holder.extraInfo != null && track.getResources().get(0).getDurationS() > 0) {
            holder.extraInfo.setText(UtilsCommon.makeTimeString(holder.itemView.getContext(),
                    track.getResources().get(0).getDurationS()));
            holder.extraInfo.setVisibility(View.VISIBLE);
        }
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, track.getName());
        } else {
            loadArtwork(artInfo, holder);
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

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset();
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

    void setLetterTileDrawable(ViewHolder holder, String text) {
        Resources resources = holder.itemView.getResources();
        LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
        drawable.setIsCircular(!gridStyle);
        holder.artwork.setImageDrawable(drawable);
    }

    void loadArtwork(ArtInfo artInfo, ViewHolder holder) {
        Bundle extras = BundleHelper.b()
                .putString(String.valueOf(lightTheme ?
                        PaletteSwatchType.VIBRANT_LIGHT : PaletteSwatchType.VIBRANT_DARK))
                .putString2(PaletteSwatchType.VIBRANT.toString())
                .putInt(holder.descriptionContainer == null ? 1 : 0) //crop circles for lists
                .get();
        presenter.getRequestor().newRequest(artInfo, holder.artwork, holder.descriptionContainer, extras);
    }

    void loadMultiArtwork(ViewHolder holder, List<ArtInfo> artInfos) {
        ArtworkRequestManager requestor = presenter.getRequestor();
        CompositeSubscription cs = holder.subscriptions;
        ImageView artwork = holder.artwork;
        ImageView artwork2 = holder.artwork2;
        ImageView artwork3 = holder.artwork3;
        ImageView artwork4 = holder.artwork4;
        ArtworkType artworkType = ArtworkType.THUMBNAIL;
        UtilsCommon.loadMultiArtwork(requestor, cs, artwork, artwork2, artwork3, artwork4, artInfos, artworkType);
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        if (inSelectionMode) {
            boolean selected = selectedItems.get(position);
            if (selected) {
                selectedItems.put(position, false);
                presenter.onItemUnselected();
                notifyItemChanged(position);
            } else {
                selectedItems.put(position, true);
                presenter.onItemSelected();
                notifyItemChanged(position);
            }
        } else {
            ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(v);
            presenter.onItemClicked(holder, getItem(position));
        }
    }

    @Override
    public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
        if (inSelectionMode) {
            return false;
        } else {
            selectedItems.put(position, true);
            inSelectionMode = true;
            presenter.onStartSelectionMode(selectionEndedListener);
            notifyItemChanged(position);
            return true;
        }
    }

    public List<Bundleable> getSelectedItems() {
        List<Bundleable> lst = new ArrayList<>();
        for (int ii=0; ii<getItemCount(); ii++) {
            if (selectedItems.get(ii)) {
                lst.add(getItem(ii));
            }
        }
        return lst;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements DragSwipeViewHolder {
        @InjectView(R.id.artwork_thumb) public ImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional public ImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional public ImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional public ImageView artwork4;
        @InjectView(R.id.grid_description) @Optional GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) @Optional TextView extraInfo;
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
            unselectedBackground = null;
            itemView.setBackground(null);
        }

        Drawable unselectedBackground;

        @Override
        public void onItemSelected() {
            itemView.setElevation(23);
            itemView.setSelected(true);
            if (descriptionContainer != null) {
                unselectedBackground = descriptionContainer.getBackground();
                descriptionContainer.setBackgroundColor(Color.BLUE);
            } else {
                unselectedBackground = itemView.getBackground();
                itemView.setBackgroundColor(Color.BLUE);
            }
        }

        @Override
        public void onItemClear() {
            itemView.setElevation(0);
            itemView.setSelected(false);
            if (descriptionContainer != null) {
                descriptionContainer.setBackground(unselectedBackground);
            } else {
                itemView.setBackground(unselectedBackground);
            }
            unselectedBackground = null;
        }

        @Override
        public View getDragHandle() {
            return dragHandle;
        }
    }

}
