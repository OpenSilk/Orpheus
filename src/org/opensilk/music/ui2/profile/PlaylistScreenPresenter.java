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

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.os.Bundle;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Func1;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class PlaylistScreenPresenter extends ProfilePresenter {
    final OverflowHandlers.Playlists playlistOverflowHandler;
    final Playlist playlist;

    @Inject
    public PlaylistScreenPresenter(ActionBarOwner actionBarOwner,
                                   ArtworkRequestManager requestor,
                                   AppPreferences settings,
                                   OverflowHandlers.Playlists playlistOverflowHandler,
                                   Playlist playlist) {
        super(actionBarOwner, requestor, settings);
        this.playlistOverflowHandler = playlistOverflowHandler;
        this.playlist = playlist;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        setupActionBar();
        loadMultiArtwork(requestor,
                playlist.mAlbumIds,
                getView().getHero(),
                getView().getHero2(),
                getView().getHero3(),
                getView().getHero4()
        );
    }

    @Override
    String getTitle(Context context) {
        return playlist.mPlaylistName;
    }

    @Override
    String getSubtitle(Context context) {
        return MusicUtils.makeLabel(context, R.plurals.Nsongs, playlist.mSongNumber);
    }

    @Override
    int getNumArtwork() {
        return playlist.mAlbumIds.length;
    }

    @Override
    ProfileAdapter makeAdapter(Context context) {
        return null;//not used
    }

    @Override
    boolean isGrid() {
        return false;//not used
    }

    PlaylistAdapter makePlaylistAdapter(Context context) {
        return new PlaylistAdapter(context, playlist.mPlaylistId);
    }

    void setupActionBar() {
        actionBarOwner.setConfig(new ActionBarOwner.Config.Builder(getCommonConfig())
                        .setMenuConfig(makeMenuConfig())
                        .build()
        );
    }

    ActionBarOwner.MenuConfig makeMenuConfig() {
        ActionBarOwner.MenuConfig.Builder b = new ActionBarOwner.MenuConfig.Builder();
        b.withMenus(OverflowHandlers.Playlists.MENUS_COMMON);
        if (!isLastAdded()) {
            b.withMenus(OverflowHandlers.Playlists.MENUS_USER);
        }
        b.setActionHandler(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer integer) {
                try {
                    return playlistOverflowHandler.handleClick(OverflowAction.valueOf(integer), playlist);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        });
        return b.build();
    }

    boolean isLastAdded() {
        return playlist.mPlaylistId == -2;
    }
}
