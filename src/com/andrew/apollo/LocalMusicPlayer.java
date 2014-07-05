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

package com.andrew.apollo;

import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.andrew.apollo.provider.MusicProviderUtil;
import com.andrew.apollo.provider.MusicStore;

import org.opensilk.cast.exceptions.NoConnectionException;
import org.opensilk.cast.exceptions.TransientNetworkDisconnectionException;

/**
 * Created by drew on 7/4/14.
 */
public class LocalMusicPlayer implements IMusicPlayer {

    private MultiPlayer mPlayer;
    private MusicPlaybackService mService;

    public LocalMusicPlayer(MusicPlaybackService service) {
        mService = service;
        mPlayer = new MultiPlayer(service);
    }

    @Override
    public long seek(long position) {
        return mPlayer.seek(position);
    }

    @Override
    public long position() {
        return mPlayer.position();
    }

    @Override
    public long duration() {
        return mPlayer.duration();
    }

    @Override
    public void play() {
        mPlayer.start();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public boolean canGoNext() {
        return true;
    }

    @Override
    public boolean isInitialized() {
        return mPlayer.isInitialized();
    }

    @Override
    public void stop(boolean goToIdle) {
        mPlayer.stop();
    }

    @Override
    public void setNextDataSource(long songId) {
        Uri uri = MusicProviderUtil.getDataUri(mService, songId);
        mPlayer.setNextDataSource(uri != null ? uri.toString() : null);
    }

    @Override
    public void setDataSource(Cursor cursor) {
        mPlayer.setDataSource(cursor.getString(cursor.getColumnIndexOrThrow(MusicStore.Cols.DATA_URI)));
    }

    @Override
    public void setDataSource(String path) {
        mPlayer.setDataSource(path);
    }

    @Override
    public long seekAndPlay(long position) {
        long seekedTo = seek(position);
        play();
        return seekedTo;
    }

    @Override
    public boolean canGoPrev() {
        return true;
    }

    @Override
    public void setNextDataSource(String path) {
        mPlayer.setNextDataSource(path);
    }

    @Override
    public void setHandler(Handler handler) {
        mPlayer.setHandler(handler);
    }

    @Override
    public void setVolume(float volume) {
        mPlayer.setVolume(volume);
    }

    public void release() {
        mPlayer.release();
    }

    public int getAudioSessionId() {
        return mPlayer.getAudioSessionId();
    }


}
