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

import com.andrew.apollo.model.LocalSong;

import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;

/**
 * Created by drew on 4/20/15.
 */
class SongsScreenAdapter extends GalleryPageAdapter<LocalSong> {

    SongsScreenAdapter(GalleryPagePresenter<LocalSong> presenter, ArtworkRequestManager artworkRequestor) {
        super(presenter, artworkRequestor);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, LocalSong song) {
        holder.title.setText(song.name);
        holder.subtitle.setText(song.artistName);
        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                null, song.albumId, ArtworkType.THUMBNAIL));
    }
}
