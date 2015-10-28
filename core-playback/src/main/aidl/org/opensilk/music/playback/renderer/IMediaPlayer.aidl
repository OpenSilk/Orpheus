// IMediaPlayer.aidl
package org.opensilk.music.playback.renderer;

// Declare any non-default types here with import statements
import android.net.Uri;
import org.opensilk.music.playback.renderer.Headers;
import org.opensilk.music.playback.renderer.IMediaPlayerCallback;

interface IMediaPlayer {
    boolean isPlaying();
    long getCurrentPosition();
    boolean setDataSource(in Uri uri, in Headers headers);
    void prepareAsync();
    void pause();
    void seekTo(long pos);
    void setVolume(float left, float right);
    void start();
    void setCallback(IMediaPlayerCallback callback);
    void reset();
    void release();
    long getDuration();
}
