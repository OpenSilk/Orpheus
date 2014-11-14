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
import android.widget.Toast;

import com.andrew.apollo.Config;
import org.opensilk.music.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.ui2.ProfileActivity;


/**
 * Various navigation helpers.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class NavUtils {

    public static void openArtistProfile(final Context context, final LocalArtist artist) {
        if (artist == null) {
            return;
        }
        // Create a new bundle to transfer the artist info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, artist);

        startProfileActivity(context, ProfileActivity.ACTION_ARTIST, b);
    }

    public static void openAlbumProfile(final Context context, final LocalAlbum album) {
        // Create a new bundle to transfer the album info
        final Bundle b = new Bundle();
        b.putString(Config.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
        b.putParcelable(Config.EXTRA_DATA, album);

        startProfileActivity(context, ProfileActivity.ACTION_ALBUM, b);
    }

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

        startProfileActivity(context, ProfileActivity.ACTION_PLAYLIST, bundle);
    }

    public static void openGenreProfile(final Context context, final Genre genre) {
        // Create a new bundle to transfer the genre info
        final Bundle bundle = new Bundle();
        bundle.putLong(Config.ID, genre.mGenreId);
        bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Genres.CONTENT_TYPE);
        bundle.putString(Config.NAME, genre.mGenreName);
        bundle.putParcelable(Config.EXTRA_DATA, genre);

        startProfileActivity(context, ProfileActivity.ACTION_GENRE, bundle);
    }

    public static void openSongGroupProfile(Context context, LocalSongGroup songGroup) {
        final Bundle b = new Bundle();
        b.putParcelable(Config.EXTRA_DATA, songGroup);
        startProfileActivity(context, ProfileActivity.ACTION_SONG_GROUP, b);
    }

    public static void startProfileActivity(final Context context, final String action, final Bundle bundle) {
        context.startActivity(
                new Intent(context, ProfileActivity.class)
                        .setAction(action)
                        .putExtra(Config.EXTRA_DATA, bundle));
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

}
