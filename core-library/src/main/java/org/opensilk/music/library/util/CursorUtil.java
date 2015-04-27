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

package org.opensilk.music.library.util;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.opensilk.music.core.model.Album;
import org.opensilk.music.core.model.Artist;
import org.opensilk.music.core.model.Folder;
import org.opensilk.music.core.model.Track;
import org.opensilk.music.core.spi.Bundleable;
import org.opensilk.music.library.proj.AlbumProj;
import org.opensilk.music.library.proj.ArtistProj;
import org.opensilk.music.library.proj.FolderProj;
import org.opensilk.music.library.proj.FolderTrackProj;
import org.opensilk.music.library.proj.TrackProj;

/**
 * Created by drew on 4/27/15.
 */
public class CursorUtil {

    public static void populateRow(MatrixCursor.RowBuilder rb, Bundleable b) {
        rb.add(b.getIdentity());
        rb.add(b.getName());
        if (b instanceof Album) {
            Album a = (Album) b;
            rb.add(a.artistName);
            rb.add(a.songCount);
            rb.add(a.date);
            rb.add(a.artworkUri);
        } else if (b instanceof Artist) {
            Artist a = (Artist) b;
            rb.add(a.albumCount);
            rb.add(a.songCount);
        } else if (b instanceof Folder) {
            Folder f = (Folder) b;
            rb.add(f.parentIdentity);
            rb.add(f.childCount);
            rb.add(f.date);
        } else if (b instanceof Track) {
            Track t = (Track) b;
            rb.add(t.albumName);
            rb.add(t.artistName);
            rb.add(t.albumArtistName);
            rb.add(t.albumIdentity);
            rb.add(t.duration);
            rb.add(t.dataUri);
            rb.add(t.artworkUri);
            rb.add(t.mimeType);
        }
    }

    public static MatrixCursor newAlbumCursor() {
        return new MatrixCursor(AlbumProj.ALL);
    }

    public static Album fromAlbumCursor(Cursor c) {
        return Album.builder()
                .setIdentity(c.getString(0))
                .setName(c.getString(1))
                .setArtistName(c.getString(2))
                .setSongCount(c.getInt(3))
                .setDate(c.getString(4))
                .setArtworkUri(!c.isNull(5) ? Uri.parse(c.getString(5)) : null)
                .build();
    }

    public static MatrixCursor newArtistCursor() {
        return new MatrixCursor(ArtistProj.ALL);
    }

    public static Artist fromArtistCursor(Cursor c) {
        return Artist.builder()
                .setIdentity(c.getString(0))
                .setName(c.getString(1))
                .setAlbumCount(c.getInt(2))
                .setSongCount(c.getInt(3))
                .build();
    }

    public static MatrixCursor newFolderCursor() {
        return new MatrixCursor(FolderProj.ALL);
    }

    public static Folder fromFolderCursor(Cursor c) {
        return Folder.builder()
                .setIdentity(c.getString(0))
                .setName(c.getString(1))
                .setParentIdentity(c.getString(2))
                .setChildCount(c.getInt(3))
                .setDate(c.getString(4))
                .build();
    }

    public static MatrixCursor newTrackCursor() {
        return new MatrixCursor(TrackProj.ALL);
    }

    public static Track fromTrackCursor(Cursor c) {
        return Track.builder()
                .setIdentity(c.getString(0))
                .setName(c.getString(1))
                .setAlbumName(c.getString(2))
                .setArtistName(c.getString(3))
                .setAlbumArtistName(c.getString(4))
                .setAlbumIdentity(c.getString(5))
                .setDuration(c.getInt(6))
                .setDataUri(Uri.parse(c.getString(7))) //never null
                .setArtworkUri(!c.isNull(8) ? Uri.parse(c.getString(8)) : null)
                .setMimeType(c.getString(9))
                .build();
    }

    public static void populateFolderTrackRow(MatrixCursor.RowBuilder rb, Bundleable b) {
        rb.add(b.getIdentity());
        rb.add(b.getName());
        if (b instanceof Folder) {
            rb.add(FolderTrackProj.KIND_FOLDER);
            Folder folder = (Folder) b;
            rb.add(folder.parentIdentity);
            rb.add(folder.childCount);
            rb.add(folder.date);
        } else {
            rb.add(FolderTrackProj.KIND_TRACK);
            Track track = (Track) b;
            rb.add(null);
            rb.add(null);
            rb.add(null);
            rb.add(track.albumName);
            rb.add(track.artistName);
            rb.add(track.albumArtistName);
            rb.add(track.albumIdentity);
            rb.add(track.duration);
            rb.add(track.dataUri);
            rb.add(track.artworkUri);
            rb.add(track.mimeType);
        }
    }

    public static MatrixCursor newFolderTrackCursor() {
        return new MatrixCursor(FolderTrackProj.ALL);
    }

    public static Bundleable fromFolderTrackCursor(Cursor c) {
        String kind = c.getString(2);
        if (kind == null) kind = "";
        switch (kind) {
            case FolderTrackProj.KIND_FOLDER:
                return Folder.builder()
                        .setIdentity(c.getString(0))
                        .setName(c.getString(1))
                        .setParentIdentity(c.getString(3)) //skip 2
                        .setChildCount(c.getInt(4))
                        .setDate(c.getString(5))
                        .build();
            case FolderTrackProj.KIND_TRACK:
                return Track.builder()
                        .setIdentity(c.getString(0))
                        .setName(c.getString(1))
                        .setAlbumName(c.getString(6)) //skip 2,3,4,5
                        .setArtistName(c.getString(7))
                        .setAlbumArtistName(c.getString(8))
                        .setAlbumIdentity(c.getString(9))
                        .setDuration(c.getInt(10))
                        .setDataUri(Uri.parse(c.getString(11))) //never null
                        .setArtworkUri(!c.isNull(12) ? Uri.parse(c.getString(12)) : null)
                        .setMimeType(c.getString(13))
                        .build();
            default:
                throw new IllegalArgumentException("Invalid kind in cursor: " + c.getString(2));
        }
    }

}
