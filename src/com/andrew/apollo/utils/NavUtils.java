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
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.ui.activities.AudioPlayerActivity;
import com.andrew.apollo.ui.activities.SearchActivity;

import org.opensilk.music.ui.activities.BaseSlidingActivity;
import org.opensilk.music.ui.activities.HomeSlidingActivity;
import org.opensilk.music.ui.profile.ProfileAlbumFragment;
import org.opensilk.music.ui.profile.ProfileArtistFragment;
import org.opensilk.music.ui.settings.SettingsActivity;

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
     * @param artistName The name of the artist
     */
    @Deprecated
    public static void openArtistProfile(final Context context, final String artistName) {

    }

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

        // We are making teh assumption that all contexts passed through were created
        // with getActivity()
        FragmentManager fm = ((BaseSlidingActivity) context).getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.main, ProfileArtistFragment.newInstance(b), "artist")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("artist")
                .commit();
        ((BaseSlidingActivity) context).maybeClosePanel();
    }

    /**
     * Opens the profile of an album.
     * 
     * @param context The {@link Activity} to use.
     * @param albumName The name of the album
     * @param artistName The name of the album artist
     * @param albumId The id of the album
     */
    @Deprecated
    public static void openAlbumProfile(final Context context,
            final String albumName, final String artistName, final long albumId) {

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
        FragmentManager fm = ((BaseSlidingActivity) context).getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.main, ProfileAlbumFragment.newInstance(b), "album")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("album")
                .commit();
        ((BaseSlidingActivity) context).maybeClosePanel();
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
                    Toast.LENGTH_LONG);
        }
    }

    /**
     * Opens to {@link SettingsActivity}.
     * 
     * @param activity The {@link Activity} to use.
     */
    public static void openSettings(final Activity activity) {
        final Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    /**
     * Opens to {@link AudioPlayerActivity}.
     * 
     * @param activity The {@link Activity} to use.
     */
    public static void openAudioPlayer(final Activity activity) {
        final Intent intent = new Intent(activity, AudioPlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Opens to {@link SearchActivity}.
     * 
     * @param activity The {@link Activity} to use.
     * @param query The search query.
     */
    public static void openSearch(final Activity activity, final String query) {
        final Bundle bundle = new Bundle();
        final Intent intent = new Intent(activity, SearchActivity.class);
        intent.putExtra(SearchManager.QUERY, query);
        intent.putExtras(bundle);
        activity.startActivity(intent);
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
