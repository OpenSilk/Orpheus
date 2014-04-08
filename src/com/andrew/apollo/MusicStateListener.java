
package com.andrew.apollo;

/**
 * Listens for playback changes to send the the fragments bound to this activity
 */
public interface MusicStateListener {

    /**
     * Called when {@link MusicPlaybackService#REFRESH} is invoked
     */
    public void restartLoader();

    /**
     * Called when {@link MusicPlaybackService#META_CHANGED} is invoked
     */
    public void onMetaChanged();

    /**
     * Called when {@link com.andrew.apollo.MusicPlaybackService#PLAYSTATE_CHANGED} is invoked
     */
    public void onPlaystateChanged();

    /**
     * Called when {@link com.andrew.apollo.MusicPlaybackService#QUEUE_CHANGED} is invoked
     */
    public void onQueueChanged();

}
