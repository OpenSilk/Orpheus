// IMediaPlayerFactory.aidl
package org.opensilk.music.playback.renderer;

// Declare any non-default types here with import statements
import org.opensilk.music.playback.renderer.IMediaPlayer;

interface IMediaPlayerFactory {
    IMediaPlayer create();
}
