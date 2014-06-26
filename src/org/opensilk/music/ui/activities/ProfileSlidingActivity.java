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

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.squareup.otto.Bus;

import org.opensilk.music.ui.profile.ProfileAlbumFragment;
import org.opensilk.music.ui.profile.ProfileArtistFragment;
import org.opensilk.music.ui.profile.ProfileGenreFragment;
import org.opensilk.music.ui.profile.ProfilePlaylistFragment;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Inject;

/**
 * Created by drew on 6/20/14.
 */
public class ProfileSlidingActivity extends BaseSlidingActivity {

    public static final String ACTION_ARTIST = "open_artist";
    public static final String ACTION_ALBUM = "open_album";
    public static final String ACTION_PLAYLIST = "open_playlist";
    public static final String ACTION_GENRE = "open_genre";

    @Inject @ForActivity
    protected Bus mActivityBus; //See comment in BaseSlidingActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getBundleExtra(Config.EXTRA_DATA);
        Fragment f = null;

        String action = getIntent().getAction();
        if (ACTION_ARTIST.equals(action)) {
            f = ProfileArtistFragment.newInstance(b);
        } else if (ACTION_ALBUM.equals(action)) {
            f = ProfileAlbumFragment.newInstance(b);
        } else if (ACTION_PLAYLIST.equals(action)) {
            f = ProfilePlaylistFragment.newInstance(b);
        } else if (ACTION_GENRE.equals(action)) {
            f = ProfileGenreFragment.newInstance(b);
        } else {
            finish();
        }

        if (savedInstanceState == null && f != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, f)
                    .commit();
        }

    }

    /*
     * Abstract methods
     */

    @Override
    protected int getLayoutId() {
        return R.layout.activity_profilesliding;
    }

    @Override
    protected Bus provideBus() {
        return mActivityBus;
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
                new ProfileModule(this),
        };
    }

}
