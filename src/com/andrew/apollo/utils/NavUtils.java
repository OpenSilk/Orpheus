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
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.ui.activities.HomeSlidingActivity;
import org.opensilk.music.ui.fragments.SearchFragment;
import org.opensilk.music.ui.profile.ProfileAlbumFragment;
import org.opensilk.music.ui.profile.ProfileArtistFragment;
import org.opensilk.music.ui.profile.ProfileGenreAlbumsFragment;
import org.opensilk.music.ui.profile.ProfileGenreFragment;
import org.opensilk.music.ui.profile.ProfilePlaylistFragment;

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
    public static void openArtistProfile(final Context context, final Artist artist) {
        if (artist == null) {
            return;
        }
        // Create a new bundle to transfer the artist info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, artist);

        replaceFragment(context, ProfileArtistFragment.newInstance(b), "artist");
    }

    /**
     * Opens Album profile activity
     * @param context
     * @param album
     */
    public static void openAlbumProfile(final Context context, final Album album) {
        // Create a new bundle to transfer the album info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, album);

        // We are making teh assumption that all contexts passed through were created
        // with getActivity()
        FragmentManager fm = ((HomeSlidingActivity) context).getSupportFragmentManager();
        int lastIndex = fm.getBackStackEntryCount() - 1;
        if (lastIndex != -1 && fm.getBackStackEntryAt(lastIndex).getName().equals(album.mAlbumName)) {
            //If the currently loaded album is the same as the one requesting, don't do it!
            return;
        }

        replaceFragment(context, ProfileAlbumFragment.newInstance(b), album.mAlbumName);
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

        replaceFragment(context, ProfilePlaylistFragment.newInstance(bundle), "playlist");
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

        replaceFragment(context, ProfileGenreFragment.newInstance(bundle), "genre");
        //replaceFragment(context, ProfileGenreAlbumsFragment.newInstance(bundle), "genre");
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
     * Replace current fragment adding transaction to backstack
     * @param context activity
     * @param fragment new fragment
     * @param name fragment and backstack entry name
     */
    private static void replaceFragment(final Context context, final Fragment fragment, final String name) {
        // We are making teh assumption that all contexts passed through were created
        // with getActivity()
        ((HomeSlidingActivity) context).hidePager();
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
    public static void goHome(final Activity activity) {
        final Intent intent = new Intent(activity, HomeSlidingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
