/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.profile.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.andrew.apollo.R;
import com.andrew.apollo.loaders.WrappedAsyncTaskLoader;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.ui.cards.SongGroupCard;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class ArtistGridLoader extends WrappedAsyncTaskLoader<List<Object>> {

    private final LocalArtist artist;

    public ArtistGridLoader(Context context, LocalArtist artist) {
        super(context);
        this.artist = artist;
    }

    @Override
    public List<Object> loadInBackground() {
        List<Object> objs = new ArrayList<>();
        // get all songs;
        Cursor c = CursorHelpers.makeArtistSongsCursor(getContext(), artist.artistId);
        final List<Long> songIds = new ArrayList<>(c.getCount());
        final Set<Long> albumIdsSet = new HashSet<>(c.getCount());
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    songIds.add(c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    albumIdsSet.add(c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
                } while (c.moveToNext());
                final Long[] songsL = songIds.toArray(new Long[songIds.size()]);
                final long[] songs = new long[songsL.length];
                for (int ii=0; ii<songsL.length; ii++) {
                    songs[ii] = songsL[ii];
                }
                final Long[] albumsL = albumIdsSet.toArray(new Long[albumIdsSet.size()]);
                final long[] albums = new long[albumsL.length];
                for (int ii=0; ii<albumsL.length; ii++) {
                    albums[ii] = albumsL[ii];
                }
                objs.add(new LocalSongGroup(getContext().getString(R.string.all_songs), artist.name, songs, albums));
            }
            c.close();
        }
        // get the albums
        c = CursorHelpers.makeLocalArtistAlbumsCursor(getContext(), artist.artistId);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    LocalAlbum album = CursorHelpers.makeLocalAlbumFromCursor(c);
                    objs.add(album);
                } while (c.moveToNext());
            }
            c.close();
        }
        return objs;
    }

}
