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

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;

/**
 * Created by drew on 4/20/15.
 */
class ArtistsScreenAdapter extends GalleryPageAdapter<LocalArtist> {

    ArtistsScreenAdapter(GalleryPagePresenter<LocalArtist> presenter, ArtworkRequestManager artworkRequestor) {
        super(presenter, artworkRequestor);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, LocalArtist artist) {
        ArtInfo artInfo = new ArtInfo(artist.name, null, null);
        holder.title.setText(artist.name);
        String subtitle = MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nalbums, artist.albumCount)
                + ", " + MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, artist.songCount);
        holder.subtitle.setText(subtitle);
        PaletteObserver paletteObserver = holder.descriptionContainer != null
                ? holder.descriptionContainer.getPaletteObserver() : null;
        holder.subscriptions.add(artworkRequestor.newArtistRequest(holder.artwork,
                paletteObserver, artInfo, ArtworkType.THUMBNAIL));
    }
}
