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

package org.opensilk.music.ui.cards.handler;

import android.support.v4.app.FragmentActivity;

import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 7/1/14.
 */
public class SongCardClickHandler {
    private final FragmentActivity activity;

    @Inject
    public SongCardClickHandler(@ForActivity FragmentActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    @Subscribe
    public void onSongCardEvent(SongCardClick e) {
        switch (e.event) {
            case PLAY:
                MusicUtils.playAllSongs(getActivity(), new Song[]{e.song}, 0, false);
                break;
            case PLAY_NEXT:
                MusicUtils.playNext(getActivity(), new Song[]{e.song});
                break;
            case ADD_TO_QUEUE:
                MusicUtils.addSongsToQueue(getActivity(), new Song[]{e.song});
                break;
            case ADD_TO_PLAYLIST:
                if (e.song instanceof LocalSong) {
                    LocalSong song = (LocalSong) e.song;
                    AddToPlaylistDialog.newInstance(new long[]{song.songId})
                            .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                }
                break;
            case MORE_BY_ARTIST:
                if (e.song instanceof LocalSong) {
                    LocalSong song = (LocalSong) e.song;
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), song.artistName));
                }
                break;
            case SET_RINGTONE:
                if (e.song instanceof LocalSong) {
                    LocalSong song = (LocalSong) e.song;
                    MusicUtils.setRingtone(getActivity(), song.songId);
                }
                break;
            case DELETE:
                if (e.song instanceof LocalSong) {
                    LocalSong song = (LocalSong) e.song;
                    DeleteDialog.newInstance(song.name, new long[]{song.songId}, null)
                            .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                }
                break;
        }
    }

}
