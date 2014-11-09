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

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.transition.ChangeImageTransform;
import android.transition.Explode;
import android.view.View;
import android.view.Window;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.LocalAlbum;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.common.mortarflow.MortarContextFactory;
import org.opensilk.common.util.VersionUtils;
import org.opensilk.common.util.ViewUtils;
import org.opensilk.music.AppModule;
import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;
import org.opensilk.music.ui.profile.AlbumFragment;
import org.opensilk.music.ui.profile.ArtistFragment;
import org.opensilk.music.ui.profile.GenreFragment;
import org.opensilk.music.ui.profile.PlaylistFragment;
import org.opensilk.music.ui.profile.SongGroupFragment;
import org.opensilk.music.ui2.details.AlbumDetailScreen;
import org.opensilk.music.ui2.gallery.BaseAdapter;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.main.MainScreen;
import org.opensilk.music.ui2.main.NavScreen;

import javax.inject.Singleton;

import butterknife.ButterKnife;
import dagger.Provides;
import flow.Parcer;
import mortar.Blueprint;

/**
 * Created by drew on 10/29/14.
 */
public class DetailActivity extends BaseSwitcherActivity {

    public static final String ACTION_ARTIST = "open_artist";
    public static final String ACTION_ALBUM = "open_album";
    public static final String ACTION_PLAYLIST = "open_playlist";
    public static final String ACTION_GENRE = "open_genre";
    public static final String ACTION_SONG_GROUP = "open_song_group";

    @Override
    protected void setupView() {
        MortarContextFactory contextFactory = new MortarContextFactory();
        View main = ViewUtils.createView(contextFactory.setUpContext(new MainScreen(), this), MainScreen.class, null);
        setContentView(main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (VersionUtils.hasLollipop()) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setSharedElementEnterTransition(new Explode());
            getWindow().setAllowEnterTransitionOverlap(true);
        }
        super.onCreate(savedInstanceState);
        AppFlow.loadInitialScreen(this);
    }

    @Override
    public Screen getDefaultScreen() {
        Parcelable parcelable = getIntent().getParcelableExtra(Config.EXTRA_DATA);
        Screen screen = null;
        String action = getIntent().getAction();
//        if (ACTION_ARTIST.equals(action)) {
//            screen = ArtistFragment.newInstance(b);
//        } else
        if (ACTION_ALBUM.equals(action)) {
            screen = new AlbumDetailScreen((LocalAlbum) parcelable);

//        } else if (ACTION_PLAYLIST.equals(action)) {
//            screen = PlaylistFragment.newInstance(b);
//        } else if (ACTION_GENRE.equals(action)) {
//            screen = GenreFragment.newInstance(b);
//        } else if (ACTION_SONG_GROUP.equals(action)) {
//            screen = SongGroupFragment.newInstance(b);
        } else {
            finish();
        }
        return screen;
    }

    public static void open(BaseAdapter.ViewHolder holder, LocalAlbum album) {
        Activity activity = findActivityContext(holder.itemView.getContext());
        if (VersionUtils.hasLollipop()) {
//            activity.getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            activity.getWindow().setSharedElementExitTransition(new Explode());
//            holder.itemView.setTransitionName("artwork");
            activity.startActivity(makeIntent(activity, album),
                    ActivityOptions.makeSceneTransitionAnimation(activity, holder.artwork, "artwork").toBundle());
        } else {
            activity.startActivity(makeIntent(activity, album));
        }
    }

    public static Intent makeIntent(Context context, LocalAlbum album) {
        return new Intent()
                .setComponent(makeComponent(context))
                .setAction(ACTION_ALBUM)
                .putExtra(Config.EXTRA_DATA, album);
    }

    static ComponentName makeComponent(Context context) {
        return new ComponentName(context, DetailActivity.class);
    }

    static Activity findActivityContext(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivityContext(((ContextWrapper) context).getBaseContext());
        } else {
            throw new IllegalArgumentException("Unknown context type: " + context.getClass());
        }
    }


}
