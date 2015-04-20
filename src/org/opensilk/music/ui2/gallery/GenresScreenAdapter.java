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

import android.content.Context;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;

/**
 * Created by drew on 4/20/15.
 */
class GenresScreenAdapter extends GalleryPageAdapter<Genre> {

    GenresScreenAdapter(GalleryPagePresenter<Genre> presenter, ArtworkRequestManager artworkRequestor) {
        super(presenter, artworkRequestor);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Genre genre) {
        holder.title.setText(genre.mGenreName);
        Context context = holder.itemView.getContext();
        String l2 = MusicUtils.makeLabel(context, R.plurals.Nalbums, genre.mAlbumNumber)
                + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, genre.mSongNumber);
        holder.subtitle.setText(l2);
        if (mGridStyle) {
            loadMultiArtwork(artworkRequestor,
                    holder.subscriptions,
                    genre.mAlbumIds,
                    holder.artwork,
                    holder.artwork2,
                    holder.artwork3,
                    holder.artwork4
            );
        } else {
            LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
            drawable.setText(genre.mGenreName);
            holder.artwork.setImageDrawable(drawable);
        }
    }

    @Override
    protected boolean multiArtwork(int position) {
        return getItem(position).mAlbumIds.length >= 2;
    }

}
