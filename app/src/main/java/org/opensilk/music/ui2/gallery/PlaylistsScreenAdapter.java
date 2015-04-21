/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.gallery;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;

/**
 * Created by drew on 4/20/15.
 */
class PlaylistsScreenAdapter extends GalleryPageAdapter<Playlist> {

    PlaylistsScreenAdapter(GalleryPagePresenter<Playlist> presenter, ArtworkRequestManager artworkRequestor) {
        super(presenter, artworkRequestor);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Playlist playlist) {
        holder.title.setText(playlist.mPlaylistName);
        holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
        if (mGridStyle) {
            loadMultiArtwork(artworkRequestor,
                    holder.subscriptions,
                    playlist.mAlbumIds,
                    holder.artwork,
                    holder.artwork2,
                    holder.artwork3,
                    holder.artwork4
            );
        } else {
            LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
            drawable.setText(playlist.mPlaylistName);
            holder.artwork.setImageDrawable(drawable);
        }
    }

    @Override
    protected boolean multiArtwork(int position) {
        return getItem(position).mAlbumIds.length >= 2;
    }
}
