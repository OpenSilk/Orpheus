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
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
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
        final Song song = e.song;
        Command c = null;
        switch (e.event) {
            case OPEN:
                c = new Command() {
                    @Override
                    public CharSequence execute() {
                        MusicUtils.playAllSongs(getActivity(), new Song[]{song}, 0, false);
                        return null;
                    }
                };
                break;
            case PLAY_NEXT:
                c = new Command() {
                    @Override
                    public CharSequence execute() {
                        MusicUtils.playNext(getActivity(), new Song[]{song});
                        return null;
                    }
                };
                break;
            case ADD_TO_QUEUE:
                c = new Command() {
                    @Override
                    public CharSequence execute() {
                        final Song[] list = new Song[]{song};
                        return MusicUtils.addSongsToQueueSilent(getActivity(), list);
                    }
                };
                break;
            case ADD_TO_PLAYLIST:
                if (song instanceof LocalSong) {
                    LocalSong localsong = (LocalSong) song;
                    AddToPlaylistDialog.newInstance(new long[]{localsong.songId})
                            .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                }
                return;
            case MORE_BY_ARTIST:
                if (song instanceof LocalSong) {
                    LocalSong localsong = (LocalSong) song;
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), localsong.artistName));
                }
                return;
            case SET_RINGTONE:
                if (song instanceof LocalSong) {
                    LocalSong localsong = (LocalSong) song;
                    MusicUtils.setRingtone(getActivity(), localsong.songId);
                }
                return;
            case DELETE:
                if (song instanceof LocalSong) {
                    LocalSong localsong = (LocalSong) song;
                    DeleteDialog.newInstance(localsong.name, new long[]{localsong.songId}, null)
                            .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                }
                return;
        }
        if (c != null) {
            new CommandRunner(getActivity(), c).execute();
        }
    }

}
