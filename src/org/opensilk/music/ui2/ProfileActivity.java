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

package org.opensilk.music.ui2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import com.andrew.apollo.Config;

import org.opensilk.music.R;
import org.opensilk.music.ui.profile.AlbumFragment;
import org.opensilk.music.ui.profile.ArtistFragment;
import org.opensilk.music.ui.profile.GenreFragment;
import org.opensilk.music.ui.profile.PlaylistFragment;
import org.opensilk.music.ui.profile.SongGroupFragment;
import org.opensilk.silkdagger.DaggerInjector;

import javax.inject.Named;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import dagger.ObjectGraph;
import dagger.Provides;
import de.greenrobot.event.EventBus;

/**
 * Created by drew on 11/8/14.
 */
public class ProfileActivity extends BaseActivity implements DaggerInjector {

    @dagger.Module(
            includes = BaseActivity.Module.class,
            injects = ProfileActivity.class
    )
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return new EventBus();
        }
    }

    public static final String ACTION_ARTIST = "open_artist";
    public static final String ACTION_ALBUM = "open_album";
    public static final String ACTION_PLAYLIST = "open_playlist";
    public static final String ACTION_GENRE = "open_genre";
    public static final String ACTION_SONG_GROUP = "open_song_group";

    ObjectGraph graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Profile_Light);
        graph = ((DaggerInjector) getApplication()).getObjectGraph().plus(new Module());
        graph.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.blank_framelayout_toolbar_overlay);
        setSupportActionBar(ButterKnife.<Toolbar>findById(this, R.id.main_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(" ");

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
    public void inject(Object o) {
        graph.inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return graph;
    }
}
