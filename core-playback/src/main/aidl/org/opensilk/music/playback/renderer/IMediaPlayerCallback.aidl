// IMediaPlayerCallback.aidl
package org.opensilk.music.playback.renderer;

// Declare any non-default types here with import statements
import org.opensilk.music.playback.renderer.IMediaPlayer;

oneway interface IMediaPlayerCallback {
    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see android.media.MediaPlayer.OnSeekCompleteListener
     */
    void onSeekComplete(IMediaPlayer mp);

    /**
     * Called when media player is done playing current song.
     *
     * @see android.media.MediaPlayer.OnCompletionListener
     */
    void onCompletion(IMediaPlayer mp);

    /**
     * Called when media player is done preparing.
     *
     * @see android.media.MediaPlayer.OnPreparedListener
     */
    void onPrepared(IMediaPlayer mp);

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see android.media.MediaPlayer.OnErrorListener
     */
    void onError(IMediaPlayer mp, String msg, int extra);

    /**
     * Invoked when the audio session id becomes known
     */
    void onAudioSessionId(IMediaPlayer mp, int audioSessionId);
}
