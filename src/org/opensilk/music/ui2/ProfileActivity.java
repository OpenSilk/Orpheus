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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.model.Playlist;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.theme.OrpheusTheme;
import org.opensilk.music.ui.profile.AlbumFragment;
import org.opensilk.music.ui.profile.GenreFragment;
import org.opensilk.music.ui.profile.PlaylistFragment;
import org.opensilk.music.ui.profile.SongGroupFragment;
import org.opensilk.music.ui2.main.Main;
import org.opensilk.music.ui2.main.QueueScreen;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.ui2.profile.AlbumScreen;
import org.opensilk.music.ui2.profile.ArtistScreen;
import org.opensilk.music.ui2.profile.GenreScreen;
import org.opensilk.music.ui2.profile.PlaylistScreen;
import org.opensilk.music.ui2.profile.SongGroupScreen;

import butterknife.ButterKnife;
import dagger.ObjectGraph;
import flow.Flow;
import flow.Layouts;
import mortar.Mortar;

/**
 * Created by drew on 11/8/14.
 */
public class ProfileActivity extends BaseSwitcherActivity implements DaggerInjector {

    public static class Blueprint extends BaseMortarActivity.Blueprint {

        public Blueprint(String scopeName) {
            super(scopeName);
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }

    }

    @dagger.Module (
            includes = {
                    BaseSwitcherActivity.Module.class,
                    Main.Module.class,
            },
            injects = ProfileActivity.class
    )
    public static class Module {

    }

    public static final String ACTION_ARTIST = "open_artist";
    public static final String ACTION_ALBUM = "open_album";
    public static final String ACTION_PLAYLIST = "open_playlist";
    public static final String ACTION_GENRE = "open_genre";
    public static final String ACTION_SONG_GROUP = "open_song_group";

    @Override
    protected mortar.Blueprint getBlueprint(String scopeName) {
        return new Blueprint(scopeName);
    }

    @Override
    protected void setupTheme() {
        OrpheusTheme orpheusTheme = mSettings.getTheme();
        setTheme(mSettings.isDarkTheme() ? orpheusTheme.profileDark : orpheusTheme.profileLight);
    }

    @Override
    public Screen getDefaultScreen() {
        return new FakeScreen();
    }

    @Override
    protected void setupView() {
        setContentView(R.layout.main_profile);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpButtonEnabled(true);
        getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        Bundle b = getIntent().getBundleExtra(Config.EXTRA_DATA);
        Screen s;

        String action = getIntent().getAction();
        if (ACTION_ARTIST.equals(action)) {
            s = new ArtistScreen(b.<LocalArtist>getParcelable(Config.EXTRA_DATA));
        } else if (ACTION_ALBUM.equals(action)) {
            s = new AlbumScreen(b.<LocalAlbum>getParcelable(Config.EXTRA_DATA));
        } else if (ACTION_PLAYLIST.equals(action)) {
            s = new PlaylistScreen(b.<Playlist>getParcelable(Config.EXTRA_DATA));
        } else if (ACTION_GENRE.equals(action)) {
            s = new GenreScreen(b.<Genre>getParcelable(Config.EXTRA_DATA));
        } else if (ACTION_SONG_GROUP.equals(action)) {
            s = new SongGroupScreen(b.<LocalSongGroup>getParcelable(Config.EXTRA_DATA));
        } else {
            finish();
            return;
        }
        MortarContextFactory cf = new MortarContextFactory();
        View v = Layouts.createView(cf.setUpContext(s, this), s);
        ButterKnife.<ViewGroup>findById(this, R.id.main).addView(v);

    }

    @Override
    public void onBackPressed() {
        if (!AppFlow.get(this).goBack()) {
            finish();
        }
    }

    @Override
    public void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback) {
        switch (direction) {
            case FORWARD:
                if (screen instanceof QueueScreen) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main, new QueueWrapperFragment())
                            .addToBackStack("queue")
                            .commit();
                }
                break;
            case BACKWARD:
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                }
                break;
            case REPLACE:
                //no replace operations here
                break;
        }
        callback.onComplete();
    }

    /*
     * DaggerInjector for fragments
     */

    @Override
    public void inject(Object o) {
        Mortar.getScope(this).getObjectGraph().inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return Mortar.getScope(this).getObjectGraph();
    }

    // Dummy screen for initial backstack
    public static class FakeScreen extends Screen {

    }

    // wrapper fragment for queue until profiles are moved to mortar
    public static class QueueWrapperFragment extends Fragment {

        MortarContextFactory contextFactory = new MortarContextFactory();
        View v;

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            v = ViewUtils.createView(contextFactory.setUpContext(new QueueScreen(), getActivity()), QueueScreen.class, null);
            return v;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            contextFactory.tearDownContext(v.getContext());
        }
    }

}
