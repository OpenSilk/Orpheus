package com.andrew.apollo;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Song;

interface IApolloService
{
    void openFile(String path);
    void open(in long [] list, int position);
    void openSongs(in Song[] list, int position);
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void enqueue(in long [] list, int action);
    void enqueueSongs(in Song[] list, int action);
    void setQueuePosition(int index);
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void moveQueueItem(int from, int to);
    void toggleFavorite();
    void refresh();
    boolean isFavorite();
    boolean isPlaying();
    Song [] getQueue();
    long duration();
    long position();
    long seek(long pos);
    String getAudioId();
    String getAlbumId();
    String getArtistName();
    String getTrackName();
    String getAlbumName();
    String getAlbumArtistName();
    String getPath();
    int getQueuePosition();
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(in Song song);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    boolean isRemotePlayback();
    ArtInfo getCurrentArtInfo();
}

