
package com.andrew.apollo;

import android.net.Uri;
import android.os.RemoteException;

import org.opensilk.music.api.meta.ArtInfo;

import java.lang.ref.WeakReference;

public final class ApolloServiceBinder extends IApolloService.Stub {

    private final WeakReference<MusicPlaybackService> mService;

    public ApolloServiceBinder(final MusicPlaybackService service) {
        mService = new WeakReference<>(service);
    }

    private MusicPlaybackService acquireService() throws RemoteException {
        MusicPlaybackService s = mService.get();
        if (s != null) {
            return s;
        }
        throw new NullPointerException("No service");
    }

    @Override
    public void openFile(final String path) throws RemoteException {
        acquireService().openFile(path);
    }

    @Override
    public void open(final long[] list, final int position) throws RemoteException {
        acquireService().open(list, position);
    }

    @Override
    public void stop() throws RemoteException {
        acquireService().stop();
    }

    @Override
    public void pause() throws RemoteException {
        acquireService().pause();
    }

    @Override
    public void play() throws RemoteException {
        acquireService().play();
    }

    @Override
    public void prev() throws RemoteException {
        acquireService().prev();
    }

    @Override
    public void next() throws RemoteException {
        acquireService().gotoNext(true);
    }

    @Override
    public void enqueue(final long[] list, final int action) throws RemoteException {
        acquireService().enqueue(list, action);
    }

    @Override
    public void setQueuePosition(final int index) throws RemoteException {
        acquireService().setQueuePosition(index);
    }

    @Override
    public void setShuffleMode(final int shufflemode) throws RemoteException {
        acquireService().setShuffleMode(shufflemode);
    }

    @Override
    public void setRepeatMode(final int repeatmode) throws RemoteException {
        acquireService().setRepeatMode(repeatmode);
    }

    @Override
    public void moveQueueItem(final int from, final int to) throws RemoteException {
        acquireService().moveQueueItem(from, to);
    }

    @Override
    public void toggleFavorite() throws RemoteException {
        acquireService().toggleFavorite();
    }

    @Override
    public void refresh() throws RemoteException {
        acquireService().refresh();
    }

    @Override
    public boolean isFavorite() throws RemoteException {
        return acquireService().isFavorite();
    }

    @Override
    public boolean isPlaying() throws RemoteException {
        return acquireService().isPlaying();
    }

    @Override
    public long[] getQueue() throws RemoteException {
        return acquireService().getQueue();
    }

    @Override
    public long duration() throws RemoteException {
        return acquireService().duration();
    }

    @Override
    public long position() throws RemoteException {
        return acquireService().position();
    }

    @Override
    public long seek(final long position) throws RemoteException {
        return acquireService().seek(position);
    }

    @Override
    public long getAudioId() throws RemoteException {
        return acquireService().getAudioId();
    }

    @Override
    public long getAlbumId() throws RemoteException {
        return acquireService().getAlbumId();
    }

    @Override
    public String getArtistName() throws RemoteException {
        return acquireService().getArtistName();
    }

    @Override
    public String getTrackName() throws RemoteException {
        return acquireService().getTrackName();
    }

    @Override
    public String getAlbumName() throws RemoteException {
        return acquireService().getAlbumName();
    }

    @Override
    public String getAlbumArtistName() throws RemoteException {
        return acquireService().getAlbumArtistName();
    }

    @Override
    public Uri getDataUri() throws RemoteException {
        return acquireService().getDataUri();
    }

    @Override
    public Uri getArtworkUri() throws RemoteException {
        return acquireService().getArtworkUri();
    }

    @Override
    public int getQueuePosition() throws RemoteException {
        return acquireService().getQueuePosition();
    }

    @Override
    public int getShuffleMode() throws RemoteException {
        return acquireService().getShuffleMode();
    }

    @Override
    public int getRepeatMode() throws RemoteException {
        return acquireService().getRepeatMode();
    }

    @Override
    public int removeTracks(final int first, final int last) throws RemoteException {
        return acquireService().removeTracks(first, last);
    }

    @Override
    public int removeTrack(final long id) throws RemoteException {
        return acquireService().removeTrack(id);
    }

    @Override
    public int getMediaMountedCount() throws RemoteException {
        return acquireService().getMediaMountedCount();
    }

    @Override
    public int getAudioSessionId() throws RemoteException {
        return acquireService().getAudioSessionId();
    }

    @Override
    public boolean isRemotePlayback() throws RemoteException {
        return acquireService().isRemotePlayback();
    }

    @Override
    public ArtInfo getCurrentArtInfo() throws RemoteException {
        return acquireService().getCurrentArtInfo();
    }

    @Override
    public boolean isFromSDCard() throws RemoteException {
        return acquireService().isFromSDCard();
    }

}
