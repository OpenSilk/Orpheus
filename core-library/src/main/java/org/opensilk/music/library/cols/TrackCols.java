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

package org.opensilk.music.library.cols;

/**
 * Created by drew on 4/26/15.
 */
public interface TrackCols extends BundleableCols {
    String ALBUM_NAME = "album_name";
    String ARTIST_NAME = "artist_name";
    String ALBUM_ARTIST_NAME = "album_artist_name";
    String ALBUM_IDENTITIY = "album_id";
    String DURATION = "duration";
    String DATA_URI = "_data";
    String ARTWORK_URI = "artwork_uri";
    String MIME_TYPE = "mime_type";
}
