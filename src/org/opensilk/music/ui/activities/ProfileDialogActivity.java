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

package org.opensilk.music.ui.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.andrew.apollo.Config;
import org.opensilk.music.R;

import org.opensilk.music.ui.profile.AlbumFragment;
import org.opensilk.music.ui.profile.ArtistFragment;
import org.opensilk.music.ui.profile.GenreFragment;
import org.opensilk.music.ui.profile.PlaylistFragment;
import org.opensilk.music.ui.profile.SongGroupFragment;

/**
 * Created by drew on 6/20/14.
 */
public class ProfileDialogActivity extends BaseSlidingDialogActivity {

    public static final String ACTION_ARTIST = "open_artist";
    public static final String ACTION_ALBUM = "open_album";
    public static final String ACTION_PLAYLIST = "open_playlist";
    public static final String ACTION_GENRE = "open_genre";
    public static final String ACTION_SONG_GROUP = "open_song_group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        mActionBarHelper.setBackground(null);
//        mActionBarHelper.setTitle(" ");

        Bundle b = getIntent().getBundleExtra(Config.EXTRA_DATA);
        Fragment f = null;

        String action = getIntent().getAction();
        if (ACTION_ARTIST.equals(action)) {
            f = ArtistFragment.newInstance(b);
        } else if (ACTION_ALBUM.equals(action)) {
            f = AlbumFragment.newInstance(b);
        } else if (ACTION_PLAYLIST.equals(action)) {
            f = PlaylistFragment.newInstance(b);
        } else if (ACTION_GENRE.equals(action)) {
            f = GenreFragment.newInstance(b);
        } else if (ACTION_SONG_GROUP.equals(action)) {
            f = SongGroupFragment.newInstance(b);
        } else {
            finish();
        }

        if (savedInstanceState == null && f != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, f)
                    .commit();
        }

    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
        };
    }

}
