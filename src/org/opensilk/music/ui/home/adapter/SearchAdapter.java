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

package org.opensilk.music.ui.home.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.music.ui.cards.AlbumCard;
import org.opensilk.music.ui.cards.ArtistCard;
import org.opensilk.music.ui.cards.SongCard;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.DaggerInjector;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;

/**
 * Created by drew on 7/22/14.
 */
public class SearchAdapter extends CardCursorAdapter {

    private final DaggerInjector mInjector;

    public SearchAdapter(Context context, DaggerInjector injector) {
        super(context);
        mInjector = injector;
    }

    @Override
    protected Card getCardFromCursor(Cursor cursor) {
        // Get the MIME type
        final String mimetype = cursor.getString(cursor
                .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));

        if (mimetype.equals("artist")) {
            // get id
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the artist name
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            // Get the album count
            final int albumCount = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
            // Get the song count
            final int songCount = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));
            // Build artist
            final LocalArtist artist = new LocalArtist(id, name, songCount, albumCount);
            final ArtistCard card = new ArtistCard(getContext(), artist);
            card.useListLayout();
            mInjector.inject(card);
            return card;
        } else if (mimetype.equals("album")) {
            // Get the Id of the album
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the album name
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            // Get the artist nam
            final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
            // generate artwork uri
            final Uri artworkUri = CursorHelpers.generateArtworkUri(id);
            // Build the album as best we can
            final LocalAlbum album = new LocalAlbum(id, name, artist, 0, null, artworkUri);
            final AlbumCard card = new AlbumCard(getContext(), album);
            card.useListLayout();
            mInjector.inject(card);
            return card;
        } else { /* audio */
            // get id
            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            // Get the track name
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            // Get the album name
            final String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            // get artist name
            final String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            // build the song as best we can
            final LocalSong song = new LocalSong(id, name, album, artist, null, 0, 0, CursorHelpers.generateDataUri(id), null, mimetype);
            final SongCard card = new SongCard(getContext(), song);
            card.useSimpleLayout();
            mInjector.inject(card);
            return card;
        }
    }
}
