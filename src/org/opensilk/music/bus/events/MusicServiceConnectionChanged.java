package org.opensilk.music.bus.events;

/**
 * Created by drew on 4/25/14.
 */
public class MusicServiceConnectionChanged {
    private final boolean connected;
    public MusicServiceConnectionChanged(boolean connected) {
        this.connected = connected;
    }
    public boolean isConnected() {
        return connected;
    }
}
