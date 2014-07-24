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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.ThemeHelper;
import com.squareup.otto.Bus;

import org.opensilk.music.ui.profile.AlbumFragment;
import org.opensilk.music.ui.profile.ArtistFragment;
import org.opensilk.music.ui.profile.GenreFragment;
import org.opensilk.music.ui.profile.PlaylistFragment;
import org.opensilk.music.ui.profile.SongGroupFragment;
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
    public static final String ACTION_SONG_GROUP = "open_song_group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getInstance(this).getProfileTheme());
//        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR);
        setupFauxDialog();
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(null);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_arrow_left_white);
        actionBar.setTitle(" ");
        actionBar.setIcon(new ColorDrawable(android.R.color.transparent));

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

    // Thanks dashclock for this
    private void setupFauxDialog() {
        // Check if this should be a dialog
        TypedValue tv = new TypedValue();
        if (!getTheme().resolveAttribute(R.attr.isDialog, tv, true) || tv.data == 0) {
            return;
        }

        // Should be a dialog; set up the window parameters.
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.profile_dialog_width);
        params.height = Math.min(
                getResources().getDimensionPixelSize(R.dimen.profile_dialog_max_height),
                dm.heightPixels * 7 / 8);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    /*
     * Abstract methods
     */

    @Override
    protected int getLayoutId() {
        return R.layout.activity_profilesliding;
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ActivityModule(this),
        };
    }

}
