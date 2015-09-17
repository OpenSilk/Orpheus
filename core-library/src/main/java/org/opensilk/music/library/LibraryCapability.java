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

package org.opensilk.music.library;

import android.os.Bundle;

import org.opensilk.music.library.provider.LibraryProviderOld;

import rx.Subscriber;

/**
 * Created by drew on 4/29/15.
 */
public interface LibraryCapability {
    /**
     * Supports querying for albums
     */
    long ALBUMS        = 1 << 0;
    /**
     * Supports querying for artists
     */
    long ARTISTS       = 1 << 1;
    /**
     * Folders tracks is for 'dumb' libraries that don't know anything
     * about meta. And {@link LibraryProviderOld#browseFolders(String, String, Subscriber, Bundle) browseFolders}
     * will only return {@link org.opensilk.music.model.Folder}s and {@link org.opensilk.music.model.Track}s.
     */
    long FOLDERSTRACKS = 1 << 2;
    /**
     * Indicates we know about meta but can only browse the 'native'
     * hierarchy. Thus {@link LibraryProviderOld#browseFolders(String, String, Subscriber, Bundle) browseFolders}
     * may return any model object. Not just {@link org.opensilk.music.model.Folder}s and
     * {@link org.opensilk.music.model.Track}s.
     */
    long FOLDERSANY    = 1 << 3;
    /**
     * Supports querying for genres
     */
    long GENRES        = 1 << 4;
    /**
     * Library supports playlists
     */
    long PLAYLISTS     = 1 << 5;
    /**
     * At a minimum all libraries must advertise this, the ability to pull track meta
     * is essential to Orpheus' functionality
     */
    long TRACKS        = 1 << 6;

    //11-20

    /**
     * Supports deleting albums, artists, folders, and tracks
     * underneath this is really only delete folders and delete tracks
     * for albums, and artists, the track list is fetched then sent back for removal
     */
    long DELETE = 1 << 11;
    /**
     * Library supports editing/deleting playlists
     */
    long EDIT_PLAYLISTS = 1 << 12;

    //21-30

    /**
     * Library has a settings activity
     */
    long SETTINGS   = 1 << 21;
    /**
     * Library requires authentication
     */
    long REQUIRES_AUTH = 1 << 22;

    //31-40

    /**
     * Library should be displayed in gallery mode
     * Library must not advertise {@link #FOLDERSTRACKS} or {@link #FOLDERSANY}
     */
    long GALLERY    = 1 << 31;
}
