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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.glide.PaletteSwatchType;
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.recycler.SelectableItemViewHolder;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.index.model.BioSummary;
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
            inSelectionMode = false;
            for (int ii=0; ii<getItemCount(); ii++) {
                if (selectedItems.get(ii)) {
                    selectedItems.put(ii, false);
                    notifyItemChanged(ii);
                }
            }
            selectedItems.clear();
        }
    };

    boolean gridStyle;
    boolean dragableList;
    boolean lightTheme;
    boolean numberTracks;

    boolean allowSelectionMode = true;

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
        return getViewHolder(inflate(parent, viewType), viewType);
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
        } else if (b instanceof BioSummary) {
            bindBio(viewHolder, (BioSummary)b);
        } else {
            Timber.e("Somehow an invalid Bundleable slipped through.");
        }
        if (viewHolder instanceof SelectableItemViewHolder) {
            if (inSelectionMode && selectedItems.get(position)) {
                ((SelectableItemViewHolder) viewHolder).onItemSelected();
            }
        }
    }

    void bindAlbum(ViewHolder holder, Album album) {
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(album.getArtistName(),
                null, album.getName(), album.getArtworkUri());
        holder.setTitle(album.getName());
        holder.setSubTitle(album.getArtistName());
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, album.getName());
        } else {
            loadArtwork(artInfo, holder);
        }
    }

    void bindArtist(ViewHolder holder, Artist artist) {
        ArtInfo artInfo = ArtInfo.forArtist(artist.getName(), null);
        holder.setTitle(artist.getName());
        Context context = holder.itemView.getContext();
        String subtitle = "";
        if (artist.getAlbumCount() > 0) {
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nalbums, artist.getAlbumCount());
        }
        if (artist.getTrackCount() > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nsongs, artist.getTrackCount());
        }
        holder.setSubTitle(subtitle);
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, artist.getName());
        } else {
            loadArtwork(artInfo, holder);
        }
    }

    void bindFolder(ViewHolder holder, Folder folder) {
        holder.setTitle(folder.getName());
        Context context = holder.itemView.getContext();
        if (folder.getChildCount() > 0) {
            holder.setSubTitle(UtilsCommon.makeLabel(context, R.plurals.Nitems, folder.getChildCount()));
        } else {
            holder.setSubTitle(" ");
        }
        if (holder instanceof HasExtraInfo) {
            ((HasExtraInfo) holder).setExtraInfo(folder.getDateModified());
        }
        setLetterTileDrawable(holder, folder.getName());
    }

    void bindGenre(ViewHolder holder, Genre genre) {
        holder.setTitle(genre.getName());
        Context context = holder.itemView.getContext();
        String l2 = UtilsCommon.makeLabel(context, R.plurals.Nalbums, genre.getAlbumsCount())
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, genre.getTracksCount());
        holder.setSubTitle(l2);
        if (gridStyle && genre.getArtInfos().size() > 0) {
            loadMultiArtwork((GridArtworkVH) holder, genre.getArtInfos());
        } else {
            setLetterTileDrawable(holder, genre.getName());
        }
    }

    void bindPlaylist(ViewHolder holder, Playlist playlist) {
        holder.setTitle(playlist.getName());
        Context context = holder.itemView.getContext();
        holder.setSubTitle(UtilsCommon.makeLabel(context, R.plurals.Nsongs, playlist.getTracksCount()));
        if (gridStyle && (playlist.getArtInfos().size() > 0)) {
            loadMultiArtwork((GridArtworkVH) holder, playlist.getArtInfos());
        } else {
            setLetterTileDrawable(holder, playlist.getName());
        }
    }

    void bindTrack(ViewHolder holder, Track track) {
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(track.getAlbumArtistName(), track.getArtistName(),
                track.getAlbumName(), track.getArtworkUri());
        holder.setTitle(track.getName());
        holder.setSubTitle(track.getArtistName());
        if ((holder instanceof HasExtraInfo) && track.getResources().get(0).getDurationS() > 0) {
            ((HasExtraInfo) holder).setExtraInfo(UtilsCommon.makeTimeString(holder.itemView.getContext(),
                    track.getResources().get(0).getDurationS()));
        }
        if (numberTracks) {
            setLetterTileDrawable(holder, String.valueOf(track.getTrackNumber()));
        } else if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, track.getName());
        } else {
            loadArtwork(artInfo, holder);
        }
    }

    void bindTrackCollection(ViewHolder holder, TrackList collection) {
        holder.setTitle(collection.getName());
        Context context = holder.itemView.getContext();
        String subtitle = "";
        if (collection.getAlbumsCount() > 0) {
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nalbums,  collection.getAlbumsCount());
        }
        if (collection.getTracksCount() > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nsongs, collection.getTracksCount());
        }
        holder.setSubTitle(subtitle);
        if (gridStyle && collection.getArtInfos().size() > 0) {
            loadMultiArtwork((GridArtworkVH) holder, collection.getArtInfos());
        } else {
            setLetterTileDrawable(holder, collection.getName());
        }
    }

    void bindBio(ViewHolder holder, BioSummary bio) {
        ((BioVH)holder).bind(bio);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    public void setGridStyle(boolean gridStyle) {
        this.gridStyle = gridStyle;
    }

    public void setNumberTracks(boolean numberTiles) {
        this.numberTracks = numberTiles;
    }

    public void setDragableList(boolean dragable) {
        this.dragableList = dragable;
    }

    public void setAllowSelectionMode(boolean allow) {
        this.allowSelectionMode = allow;
    }

    protected boolean wantsMultiArtwork(Bundleable item) {
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
        if (holder instanceof HasArtwork) {
            ImageView artwork = ((HasArtwork) holder).getArtwork();
            Resources resources = artwork.getResources();
            LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
            drawable.setIsCircular(!gridStyle);
            artwork.setImageDrawable(drawable);
        }
    }

    void loadArtwork(ArtInfo artInfo, ViewHolder holder) {
        if (holder instanceof HasArtwork) {
            boolean isgridartwork = holder instanceof GridArtworkVH;
            Bundle extras = BundleHelper.b()
                    .putString(String.valueOf(lightTheme ?
                            PaletteSwatchType.VIBRANT_LIGHT : PaletteSwatchType.VIBRANT_DARK))
                    .putString2(PaletteSwatchType.VIBRANT.toString())
                    .putInt(isgridartwork ? 0 : 1) //crop circles for lists
                    .get();
            ImageView artwork = ((HasArtwork) holder).getArtwork();
            if (isgridartwork) {
                presenter.getRequestor().newRequest(artInfo, artwork,
                        ((GridArtworkVH)holder).descriptionContainer, extras);
            } else {
                presenter.getRequestor().newRequest(artInfo, artwork, extras);
            }
        }
    }

    void loadMultiArtwork(GridArtworkVH holder, List<ArtInfo> artInfos) {
        ArtworkRequestManager requestor = presenter.getRequestor();
        ImageView artwork = holder.artwork;
        ImageView artwork2 = holder.artwork2;
        ImageView artwork3 = holder.artwork3;
        ImageView artwork4 = holder.artwork4;
        UtilsCommon.loadMultiArtwork(requestor, artwork, artwork2, artwork3, artwork4, artInfos);
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(v);
        if (inSelectionMode) {
            if (holder instanceof SelectableItemViewHolder) {
                boolean selected = selectedItems.get(position);
                if (selected) {
                    selectedItems.put(position, false);
                    notifyItemChanged(position);
                    presenter.onItemUnselected();
                } else {
                    selectedItems.put(position, true);
                    notifyItemChanged(position);
                    presenter.onItemSelected();
                }
            }
        } else {
            presenter.onItemClicked(holder, getItem(position));
        }
    }

    @Override
    public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
        ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(v);
        if (allowSelectionMode && !inSelectionMode) {
            if (holder instanceof SelectableItemViewHolder) {
                selectedItems.put(position, true);
                inSelectionMode = true;
                notifyItemChanged(position);
                presenter.onStartSelectionMode(selectionEndedListener);
            }
        }
        return true;
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

    @Override
    public int getItemViewType(int position) {
        Bundleable item = getItem(position);
        if (gridStyle) {
            if (item instanceof BioSummary) {
                return R.layout.gallery_list_item_bio_summary;
            } else if (wantsMultiArtwork(item)) {
                return R.layout.gallery_grid_item_artwork4;
            } else {
                return R.layout.gallery_grid_item_artwork;
            }
        } else {
            if (item instanceof BioSummary) {
                return R.layout.gallery_list_item_bio_summary;
            } else if (dragableList) {
                return R.layout.gallery_list_item_dragsort;
            } else {
                return R.layout.gallery_list_item_artwork;
            }
        }
    }

    private ViewHolder getViewHolder(View itemView, int id) {
        switch (id) {
            case R.layout.gallery_grid_item_artwork:
            case R.layout.gallery_grid_item_artwork4:
                return new GridArtworkVH(itemView);
            case R.layout.gallery_list_item_artwork:
                return new ListArtworkVH(itemView);
            case R.layout.gallery_list_item_dragsort:
                return new ListArtworkDragVH(itemView);
            case R.layout.gallery_list_item_bio_summary:
                return new BioVH(itemView);
            default:
                throw new IllegalArgumentException("Unknown layout id");
        }
    }

    public static class BioVH extends ViewHolder {
        @InjectView(R.id.summary_text) TextView summary;
        public BioVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        public void bind(final BioSummary bio) {
            summary.setText(Html.fromHtml(bio.getSummary()));
        }

        @Override
        public void reset() {
            super.reset();
        }
    }

    public static class GridArtworkVH extends ViewHolder implements HasArtwork, SelectableItemViewHolder {
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional public ImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional public ImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional public ImageView artwork4;
        @InjectView(R.id.grid_description) GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        private int descColor;
        private int descTitleColor;
        private int descSubTitleColor;

        public GridArtworkVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            Drawable descBackground = descriptionContainer.getBackground();
            if (descBackground instanceof ColorDrawable) {
                descColor = ((ColorDrawable) descBackground).getColor();
            } else {
                descColor = ThemeUtils.getThemeAttrColor(itemView.getContext(),
                        android.R.attr.colorBackground);
            }
            descTitleColor = descriptionContainer.getTitle().getCurrentTextColor();
            descSubTitleColor = descriptionContainer.getSubTitle().getCurrentTextColor();
        }

        public void reset() {
            super.reset();
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            if (descriptionContainer != null) {
                descriptionContainer.setBackgroundColor(descColor);
                descriptionContainer.getTitle().setTextColor(descTitleColor);
                descriptionContainer.getSubTitle().setTextColor(descSubTitleColor);
            }
        }

        @Override
        public void onItemSelected() {
            int selectedColor = ThemeUtils.getColorAccent(itemView.getContext());
            descriptionContainer.setBackgroundColor(selectedColor);
        }

        @Override
        public void onItemClear() {
            descriptionContainer.setBackgroundColor(descColor);
        }

        @Override
        public ImageView getArtwork() {
            return artwork;
        }

        @Override
        public void setTitle(CharSequence text) {
            title.setText(text);
        }

        @Override
        public void setSubTitle(CharSequence tetx) {
            subtitle.setText(tetx);
        }
    }

    public static class ListArtworkVH extends ViewHolder implements HasArtwork, HasExtraInfo, SelectableItemViewHolder {
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) @Optional TextView extraInfo;

        public ListArtworkVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        @Override
        public void reset() {
            super.reset();
            if (extraInfo != null && extraInfo.getVisibility() != View.GONE) extraInfo.setVisibility(View.GONE);
            if (VersionUtils.hasJellyBean()) {
                clearItemBackground16();
            } else {
                //noinspection deprecation
                itemView.setBackgroundDrawable(null);
            }
        }

        @Override
        public void onItemSelected() {
            int selectedColor = ThemeUtils.getColorAccent(itemView.getContext());
            itemView.setBackgroundColor(selectedColor);
        }

        @Override
        public void onItemClear() {
            if (VersionUtils.hasJellyBean()) {
                clearItemBackground16();
            } else {
                //noinspection deprecation
                itemView.setBackgroundDrawable(null);
            }
        }

        @Override
        public ImageView getArtwork() {
            return artwork;
        }

        @Override
        public void setTitle(CharSequence text) {
            title.setText(text);
        }

        @Override
        public void setSubTitle(CharSequence tetx) {
            subtitle.setText(tetx);
        }

        @Override
        public void setExtraInfo(CharSequence text) {
            if (extraInfo != null && !StringUtils.isEmpty(text)) {
                extraInfo.setText(text);
                extraInfo.setVisibility(View.VISIBLE);
            }
        }

        @TargetApi(16)
        private void clearItemBackground16() {
            itemView.setBackground(null);
        }
    }

    public static class ListArtworkDragVH extends ListArtworkVH implements DragSwipeViewHolder {
        @InjectView(R.id.drag_handle) View dragHandle;

        public ListArtworkDragVH(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        @Override
        public View getDragHandle() {
            return dragHandle;
        }

    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder  {
        public ViewHolder(View itemView) {
            super(itemView);
        }
        public void reset() {
            itemView.setSelected(false);
        }
        public void setTitle(CharSequence text) { }
        public void setSubTitle(CharSequence tetx) { }
    }

    interface HasArtwork {
        ImageView getArtwork();
    }

    interface HasExtraInfo {
        void setExtraInfo(CharSequence text);
    }

}
