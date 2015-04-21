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

import com.andrew.apollo.model.LocalAlbum;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;

/**
 * Created by drew on 4/20/15.
 */
class AlbumsScreenAdapter extends GalleryPageAdapter<LocalAlbum> {

    AlbumsScreenAdapter(GalleryPagePresenter<LocalAlbum> presenter, ArtworkRequestManager artworkRequestor) {
        super(presenter, artworkRequestor);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, LocalAlbum album) {
        ArtInfo artInfo = new ArtInfo(album.artistName, album.name, album.artworkUri);
        holder.title.setText(album.name);
        holder.subtitle.setText(album.artistName);
        PaletteObserver paletteObserver = holder.descriptionContainer != null
                ? holder.descriptionContainer.getPaletteObserver() : null;
        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                paletteObserver, artInfo, ArtworkType.THUMBNAIL));
    }

}
