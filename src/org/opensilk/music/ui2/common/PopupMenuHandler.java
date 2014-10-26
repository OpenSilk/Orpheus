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

package org.opensilk.music.ui2.common;

import android.view.MenuItem;
import android.widget.PopupMenu;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.R;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui.cards.event.CardEvent;

/**
 * Created by drew on 10/24/14.
 */
public class PopupMenuHandler {

    public static void populateMenu(PopupMenu m, Album a) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (a instanceof LocalAlbum) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                CardEvent event = CardEvent.valueOf(item.getItemId());
                return true;
            }
        });
    }

    public static void populateMenu(PopupMenu m, Artist a) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (a instanceof LocalArtist) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_delete);
        }
    }

    public static void populateMenu(PopupMenu m, Genre g) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        m.inflate(R.menu.popup_add_to_playlist);
    }

    public static void populateMenu(PopupMenu m, Playlist p) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (p.mPlaylistId != -2) {
            // cant rename or delete last added
            m.inflate(R.menu.popup_rename);
            m.inflate(R.menu.popup_delete);
        }
    }

    public static void populateMenu(PopupMenu m, Song s) {
        m.inflate(R.menu.popup_play_next);
        m.inflate(R.menu.popup_add_to_queue);
        if (s instanceof LocalSong) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_set_ringtone);
            m.inflate(R.menu.popup_delete);
        }
    }

}
