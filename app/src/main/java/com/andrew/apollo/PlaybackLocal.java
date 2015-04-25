/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package com.andrew.apollo;

import android.media.MediaCodec;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;

import java.io.File;

import timber.log.Timber;

/**
 * Created by drew on 4/23/15.
 */
public class PlaybackLocal {

//    final PlaybackService mService;
//
//    public PlaybackLocal(PlaybackService mService) {
//        this.mService = mService;
//    }
    final MusicPlaybackService mService;

    public PlaybackLocal(MusicPlaybackService mService) {
        this.mService = mService;
    }

    ExoPlayer exoPlayer;

    void play() {
        FrameworkSampleExtractor extractor = new FrameworkSampleExtractor(mService,
                Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"test1.mp3")), null);
        DefaultSampleSource source = new DefaultSampleSource(extractor, 1);
        MediaCodecAudioTrackRenderer renderer = new MediaCodecAudioTrackRenderer(source, new Handler(), new MediaCodecAudioTrackRenderer.EventListener() {
            @Override
            public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
                Timber.e(e, "onAudioTrackInitializationError");
            }

            @Override
            public void onAudioTrackWriteError(AudioTrack.WriteException e) {
                Timber.e(e, "onAudoiTrackWriteError");
            }

            @Override
            public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
                Timber.e(e, "onDecoderInitializationError");
            }

            @Override
            public void onCryptoError(MediaCodec.CryptoException e) {
                Timber.e(e, "onCryptoError");
            }
        });
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        exoPlayer = ExoPlayer.Factory.newInstance(1);
        exoPlayer.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean b, int i) {
                Timber.d("onPlayerStateCHanged(%s, %d)", b, i);
            }

            @Override
            public void onPlayWhenReadyCommitted() {
                Timber.d("onPlayWhenReadycommited");
            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
                Timber.e(e, "onPlayerError");
            }
        });
        exoPlayer.prepare(renderer);
        exoPlayer.setPlayWhenReady(true);
    }
}
