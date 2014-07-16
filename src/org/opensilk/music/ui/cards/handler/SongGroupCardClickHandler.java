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

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.SongGroupCardClick;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 7/10/14.
 */
public class SongGroupCardClickHandler {
    private final FragmentActivity activity;

    @Inject
    public SongGroupCardClickHandler(@ForActivity FragmentActivity activity) {
        this.activity = activity;
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    @Subscribe
    public void onSongGroupCardClick(SongGroupCardClick e) {
        final LocalSongGroup group = e.songGroup;
        Command command = null;
        switch (e.event) {
            case OPEN:
                NavUtils.openSongGroupProfile(getActivity(), group);
                return;
            case PLAY_ALL:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = MusicUtils.getLocalSongList(getActivity(), group.songIds);
                        MusicUtils.playAllSongs(getActivity(), list, 0, false);
                        return null;
                    }
                };
                break;
            case SHUFFLE_ALL:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = MusicUtils.getLocalSongList(getActivity(), group.songIds);
                        MusicUtils.playAllSongs(getActivity(), list, 0, true);
                        return null;
                    }
                };
                break;
            case ADD_TO_QUEUE:
                command = new Command() {
                    @Override
                    public CharSequence execute() {
                        LocalSong[] list = MusicUtils.getLocalSongList(getActivity(), group.songIds);
                        return MusicUtils.addSongsToQueueSilent(getActivity(), list);
                    }
                };
                break;
            case ADD_TO_PLAYLIST:
                AddToPlaylistDialog.newInstance(group.songIds)
                        .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                return;
        }
        if (command != null) {
            ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
        }
    }
}
