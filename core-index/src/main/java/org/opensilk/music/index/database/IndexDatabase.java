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

package org.opensilk.music.index.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.List;

/**
 * Created by drew on 9/13/15.
 */
public interface IndexDatabase {
    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy);
    int delete(String table, String whereClause, String[] whereArgs);
    long insert(String table, String nullColumnHack, ContentValues values,int conflictAlgorithm);
    int update(String table, ContentValues values, String whereClause, String[] whereArgs);
    List<Artist> getAlbumArtists(String sortOrder);
    List<Artist> getArtists(String sortOrder);
    List<Album> getArtistAlbums(String id, String sortOrder);
    List<Model> getArtistDetails(String id, String sortOrder);
    List<Track> getArtistTracks(String id, String sortOrder);
    @Nullable Artist getArtist(String id);
    @Nullable String getArtistMbid(String id);
    List<Album> getAlbums(String sortOrder);
    List<Track> getAlbumTracks(String id, String sortOrder);
    List<Model> getAlbumDetails(String id, String sortOrder);
    @Nullable Album getAlbum(String id);
    @Nullable String getAlbumMbid(String id);
    List<Track> getTracks(String sortOrder);
    List<Track> getTracks(String sortOrder, boolean excludeOrphaned);
    List<Track> getTracksInList(List<Uri> uris);
    Track getTrack(Uri uri);
    List<Genre> getGenres(String sortOrder);
    List<Album> getGenreAlbums(String id, String sortOrder);
    List<Model> getGenreDetails(String id, String sortOrder);
    List<Track> getGenreTracks(String id, String sortOrder);
    List<Playlist> getPlaylists(String sortOrder);
    Playlist getPlaylist(String id);
    List<Track> getPlaylistTracks(String id, String sortOrder);
    long insertPlaylist(String name);
    int addToPlaylist(String playlist_id, List<Uri> uriList);
    int movePlaylistEntry(String playlist_id, int from, int to);
    int removeFromPlaylist(String playlist_id, Uri uri);
    int removeFromPlaylist(String playlist_id, int pos);
    int updatePlaylist(String playlist_id, List<Uri> uriList);
    int removePlaylists(String[] playlist_ids);

    long hasContainer(Uri uri);
    @NonNull List<Pair<Uri, Uri>> findTopLevelContainers(@Nullable String authority);
    int removeContainersInError(@Nullable String authority);
    boolean markContainerInError(Uri uri);
    long insertContainer(Uri uri, Uri parentUri);
    int removeContainer(Uri uri);
    long insertTrack(Track track, Metadata metadata);
    boolean trackNeedsScan(Track track);
    TreeNode buildTree(Uri uri, Uri parentUri);
    boolean removeTrack(Uri uri, Uri parentUri);

    List<Uri> getLastQueue();
    void saveQueue(List<Uri> queue);
    int getLastQueuePosition();
    void saveQueuePosition(int pos);
    int getLastQueueShuffleMode();
    void saveQueueShuffleMode(int mode);
    int getLastQueueRepeatMode();
    void saveQueueRepeatMode(int mode);
    long getLastSeekPosition();
    void saveLastSeekPosition(long pos);

}
