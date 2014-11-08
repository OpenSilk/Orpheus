/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.andrew.apollo.Config;
import org.opensilk.music.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.dialogs.SleepTimerDialog;
import org.opensilk.music.ui.activities.HomeSlidingActivity;
import org.opensilk.music.ui.activities.ProfileDialogActivity;
import org.opensilk.music.ui.folder.FolderFragment;
import org.opensilk.music.ui.home.SearchFragment;
import org.opensilk.music.ui.library.LibraryFragment;
import org.opensilk.music.ui.settings.SettingsActivity;
import org.opensilk.music.util.MarkedForRemoval;

import timber.log.Timber;

import static android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN;


/**
 * Various navigation helpers.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class NavUtils {

    /**
     * Opens the profile of an artist.
     *
     * @param context The {@link Activity} to use.
     * @param artist The artist object
     */
    public static void openArtistProfile(final Context context, final LocalArtist artist) {
        if (artist == null) {
            return;
        }
        // Create a new bundle to transfer the artist info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, artist);

        startProfileActivity(context, ProfileDialogActivity.ACTION_ARTIST, b);
    }

    /**
     * Opens Album profile activity
     * @param context
     * @param album
     */
    public static void openAlbumProfile(final Context context, final LocalAlbum album) {
        // Create a new bundle to transfer the album info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, album);

        // Completely insane method to prevent 'endless profiles' resulting
        // from clicking the artwork thumbnail in the sliding pane over and over
        if (context instanceof ProfileDialogActivity) {
            ProfileDialogActivity activity = (ProfileDialogActivity) context;
            if (ProfileDialogActivity.ACTION_ALBUM.equals(activity.getIntent().getAction())) {
                Bundle b2 = activity.getIntent().getBundleExtra(Config.EXTRA_DATA);
                if (b2 != null) {
                    LocalAlbum a = b2.getParcelable(Config.EXTRA_DATA);
                    if (a != null) {
                        if (album.equals(a)) {
                            return;
                        }
                    }
                }
            }
        }

        startProfileActivity(context, ProfileDialogActivity.ACTION_ALBUM, b);
    }

    /**
     * Opens playlist fragment
     * @param context
     * @param playlist
     */
    public static void openPlaylistProfile(final Context context, final Playlist playlist) {
        final Bundle bundle = new Bundle();
        String playlistName;
        if (playlist.mPlaylistId == -1) { // Favorites list
            return;
//                    playlistName = getContext().getString(R.string.playlist_favorites);
//                    bundle.putString(Config.MIME_TYPE, getContext().getString(R.string.playlist_favorites));
        } else if (playlist.mPlaylistId == -2) { // Last added
            playlistName = context.getString(R.string.playlist_last_added);
            bundle.putString(Config.MIME_TYPE, context.getString(R.string.playlist_last_added));
        } else { // User created
            playlistName = playlist.mPlaylistName;
            bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Playlists.CONTENT_TYPE);
            bundle.putLong(Config.ID, playlist.mPlaylistId);
        }

        bundle.putString(Config.NAME, playlistName);
        bundle.putParcelable(Config.EXTRA_DATA, playlist);

        startProfileActivity(context, ProfileDialogActivity.ACTION_PLAYLIST, bundle);
    }

    /**
     * Opens genre fragment
     * @param context
     * @param genre
     */
    public static void openGenreProfile(final Context context, final Genre genre) {
        // Create a new bundle to transfer the genre info
        final Bundle bundle = new Bundle();
        bundle.putLong(Config.ID, genre.mGenreId);
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Genres.CONTENT_TYPE);
        bundle.putString(Config.NAME, genre.mGenreName);
        bundle.putParcelable(Config.EXTRA_DATA, genre);

        startProfileActivity(context, ProfileDialogActivity.ACTION_GENRE, bundle);
    }

    public static void openSongGroupProfile(Context context, LocalSongGroup songGroup) {
        final Bundle b = new Bundle();
        b.putParcelable(Config.EXTRA_DATA, songGroup);
        startProfileActivity(context, ProfileDialogActivity.ACTION_SONG_GROUP, b);
    }

    /**
     * Opens to {@link SearchActivity}.
     *
     * @param context The {@link Activity} to use.
     */
    public static void openSearch(final Context context) {
        replaceFragment(context, new SearchFragment(), "search");
    }

    /**
     * Starts the profile activity with given action and bundle extra
     * @param context
     * @param action
     * @param bundle
     */
    public static void startProfileActivity(final Context context, final String action, final Bundle bundle) {
        context.startActivity(new Intent(context, ProfileDialogActivity.class)
                .setAction(action).putExtra(Config.EXTRA_DATA, bundle));
    }

    /**
     * Replace current fragment adding transaction to backstack
     * @param context activity
     * @param fragment new fragment
     * @param name fragment and backstack entry name
     */
    private static void replaceFragment(final Context context, final Fragment fragment, final String name) {
        // We are making teh assumption that all contexts passed through were created
        // with getActivity()
        FragmentManager fm = ((HomeSlidingActivity) context).getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.main, fragment, name)
                .setTransition(TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(name)
                .commit();
        ((HomeSlidingActivity) context).maybeClosePanel();
    }

    /**
     * Opens the sound effects panel or DSP manager in CM
     * 
     * @param context The {@link Activity} to use.
     */
    public static void openEffectsPanel(final Activity context) {
        try {
            final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getAudioSessionId());
            effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
            context.startActivityForResult(effects, 0);
        } catch (final ActivityNotFoundException notFound) {
            Toast.makeText(context, context.getString(R.string.no_effects_for_you),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Opens to {@link org.opensilk.music.ui.activities.HomeSlidingActivity}.
     * 
     * @param activity The {@link Activity} to use.
     */
    @Deprecated @MarkedForRemoval
    public static void goHome(FragmentActivity activity) {
    }

    public static void openFoldersFragment(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        maybeClearBackstack(fm);
        fm.beginTransaction().replace(R.id.main, new FolderFragment(), "folders").commit();
    }

    public static void openSettings(Activity activity) {
        activity.startActivityForResult(new Intent(activity, SettingsActivity.class), 0);
    }

    public static void openLibrary(FragmentActivity activity, PluginInfo info) {
        FragmentManager fm = activity.getSupportFragmentManager();
        maybeClearBackstack(fm);
        fm.beginTransaction().replace(R.id.main, LibraryFragment.newInstance(info), "library").commit();
    }

    public static void maybeClearBackstack(FragmentManager fm) {
        if (fm.getBackStackEntryCount() > 0) {
            try {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } catch (IllegalStateException e) {
                Timber.e(e, "maybeClearBackstack()::popBackStackImmediate");
            }
        }
    }

    public static void openSleepTimerDialog(FragmentActivity context) {
        new SleepTimerDialog().show(context.getSupportFragmentManager(), "SleepTimer");
    }
}
