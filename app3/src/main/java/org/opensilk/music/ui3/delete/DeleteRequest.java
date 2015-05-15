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

package org.opensilk.music.ui3.delete;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 5/15/15.
 */
public class DeleteRequest implements Parcelable {
    final String name;
    final Uri tracksUri;
    final Uri callUri;
    final List<Uri> trackUrisList;
    final Uri notifyUri;

    private DeleteRequest(String name, Uri tracksUri, Uri callUri, List<Uri> trackUrisList, Uri notifyUri) {
        this.name = name;
        this.tracksUri = tracksUri;
        this.callUri = callUri;
        this.trackUrisList = trackUrisList;
        this.notifyUri = notifyUri;
    }

    public static DeleteRequest forAlbum(String authority, String libraryId, Album album) {
        Uri uri = LibraryUris.albumTracks(authority, libraryId, album.identity);
        Uri notifyUri = LibraryUris.albums(authority, libraryId);
        return new DeleteRequest(album.name, uri, null, null, notifyUri);
    }

    public static DeleteRequest forArtist(String authority, String libraryId, Artist artist) {
        Uri uri = LibraryUris.artistTracks(authority, libraryId, artist.identity);
        Uri notifyUri = LibraryUris.artists(authority, libraryId);
        return new DeleteRequest(artist.name, uri, null, null, notifyUri);
    }

    public static DeleteRequest forFolder(String authority, String libraryId, Folder folder) {
        Uri uri = LibraryUris.folders(authority, libraryId, folder.identity);
        Uri notifyUri = LibraryUris.folders(authority, libraryId, folder.parentIdentity);
        return new DeleteRequest(folder.name, null, uri, null, notifyUri);
    }

    public static DeleteRequest forPlaylist(String authority, String libraryId, Playlist playlist) {
        Uri uri = LibraryUris.playlist(authority, libraryId, playlist.identity);
        Uri notifyUri = LibraryUris.playlists(authority, libraryId);
        return new DeleteRequest(playlist.name, null, uri, null, notifyUri);
    }

    public static DeleteRequest forTrack(String authority, String libraryId, Track track, Uri notifyUri) {
        Uri uri = LibraryUris.track(authority, libraryId, track.identity);
        return new DeleteRequest(track.name, null, null, Collections.singletonList(uri), notifyUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(tracksUri, flags);
        dest.writeParcelable(callUri, flags);
        dest.writeTypedList(trackUrisList);
        dest.writeParcelable(notifyUri, flags);
    }

    public static final Creator<DeleteRequest> CREATOR = new Creator<DeleteRequest>() {
        @Override
        public DeleteRequest createFromParcel(Parcel source) {
            return new DeleteRequest(
                    source.readString(),
                    source.<Uri>readParcelable(getClass().getClassLoader()),
                    source.<Uri>readParcelable(getClass().getClassLoader()),
                    source.<Uri>createTypedArrayList(Uri.CREATOR),
                    source.<Uri>readParcelable(getClass().getClassLoader())
            );
        }

        @Override
        public DeleteRequest[] newArray(int size) {
            return new DeleteRequest[size];
        }
    };
}
