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

package org.opensilk.music.playback;

import org.opensilk.music.model.Track;
import org.opensilk.music.playback.player.PlayerStatus;

import javax.inject.Inject;

/**
 * Created by drew on 5/7/15.
 */
public class PlaybackStatus {
    //last known seek position
    long currentSeekPos = -1;
    //last know duration
    long currentDuration = -1;
    //last known queu position
    int currentQueuePos = -1;
    //
    boolean repeatOn;
    //currently playing track
    Track currentTrack;
    //next track to load
    Track nextTrack;
    //trou if we should be playing
    boolean supposedToBePlaying;
    //trou if we should start playing again when we regain audio focus
    boolean pausedByTransientLossOfFocus;
    //true if we should start playing when loading finishes
    boolean playWhenReady;
    //PlaybackStatus.* things
    int playerState = PlayerStatus.NONE;

    @Inject
    public PlaybackStatus() {

    }

    public void reset() {
        currentSeekPos = -1;
        currentDuration = -1;
        currentQueuePos = -1;
        currentTrack = null;
        nextTrack = null;
        supposedToBePlaying = false;
        pausedByTransientLossOfFocus = false;
        playWhenReady = false;
        playerState = PlayerStatus.NONE;
    }

    public void setCurrentSeekPos(long currentSeekPos) {
        this.currentSeekPos = currentSeekPos;
    }

    public long getCurrentSeekPos() {
        return currentSeekPos;
    }

    public void setCurrentDuration(long currentDuration) {
        this.currentDuration = currentDuration;
    }

    public long getCurrentDuration() {
        return currentDuration;
    }

    public void setCurrentTrack(Track currentTrack) {
        this.currentTrack = currentTrack;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void setNextTrack(Track nextTrack) {
        this.nextTrack = nextTrack;
    }

    public Track getNextTrack() {
        return nextTrack;
    }

    public void setNextTrackToCurrent() {
        currentTrack = nextTrack;
        nextTrack = null;
    }

    public int getCurrentQueuePos() {
        return currentQueuePos;
    }

    public void setCurrentQueuePos(int currentQueuePos) {
        this.currentQueuePos = currentQueuePos;
    }

    public void setIsSupposedToBePlaying(boolean supposedToBePlaying) {
        this.supposedToBePlaying = supposedToBePlaying;
    }

    public boolean isSupposedToBePlaying() {
        return supposedToBePlaying;
    }

    public void setPausedByTransientLossOfFocus(boolean pausedByTransientLossOfFocus) {
        this.pausedByTransientLossOfFocus = pausedByTransientLossOfFocus;
    }

    public boolean isPausedByTransientLossOfFocus() {
        return pausedByTransientLossOfFocus;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        this.playWhenReady = playWhenReady;
    }

    public boolean shouldPlayWhenReady() {
        return playWhenReady;
    }

    public void setPlayerState(int playerStatus) {
        this.playerState = playerStatus;
    }

    public boolean isPlayerReady() {
        return playerState == PlayerStatus.READY;
    }

    public boolean isPlayerLoading() {
        return playerState == PlayerStatus.LOADING;
    }
}
