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

package org.opensilk.music.artwork;

import android.os.ParcelFileDescriptor;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.api.meta.ArtInfo;

import rx.Subscription;

/**
 * Created by drew on 10/22/14.
 */
public interface ArtworkRequestManager {

    Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, ArtInfo artInfo, ArtworkType artworkType);
    Subscription newAlbumRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, long albumId, ArtworkType artworkType);
    Subscription newArtistRequest(AnimatedImageView imageView, PaletteObserver paletteObserver, ArtInfo artInfo, ArtworkType artworkType);

    ParcelFileDescriptor getArtwork(String artistName, String albumName);
    ParcelFileDescriptor getArtworkThumbnail(String artistName, String albumName);

    boolean clearCaches();
    void evictL1();

}
