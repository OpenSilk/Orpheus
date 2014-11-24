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
package org.opensilk.music.cast;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.support.v7.media.MediaRouter;

import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.provider.MusicStore;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import hugo.weaving.DebugLog;

import static org.opensilk.cast.helpers.RemoteCastServiceManager.sCastService;

/**
 * Created by drew on 2/13/14.
 */
public class CastUtils {

    private CastUtils() {
        //static
    }

    /**
     * Increments volume on remote device by delta
     * @param increment delta
     */
    public static void changeRemoteVolume(double increment) {
        if (sCastService != null) {
            try {
                sCastService.getCastManager().changeVolume(increment);
            } catch (final RemoteException ignored) {
            }
        }
    }

    public static float getRemoteVolume() {
        if (sCastService != null) {
            try {
                return sCastService.getCastManager().getVolume();
            } catch (RemoteException ignored) {}
        }
        return 1.0f;
    }

    /**
     * Called when user selects a device with the cast icon, we do some stuff then
     * notify the service so it can instruct the cast manager to connect
     * @param context
     * @param info
     * @return true if we notified the service, false if we failed
     */
    public static boolean notifyRouteSelected(Context context, MediaRouter.RouteInfo info) {
        if (sCastService != null) {
            try {
//                if (sCastService.getCastManager().getReconnectionStatus() == ReconnectionStatus.FINALIZE) {
//                    sCastService.getCastManager().setReconnectionStatus(ReconnectionStatus.INACTIVE);
//                    return true;
//                }
//                Utils.saveStringToPreference(context, BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
                sCastService.getCastManager().getRouteListener().onRouteSelected(info.getExtras());
                return true;
            } catch (final RemoteException ignored) {

            }
        }
        return false;
    }

    /**
     * Tell the service we just disconnected from the remote device
     */
    public static void notifyRouteUnselected() {
        if (sCastService != null) {
            try {
                sCastService.getCastManager().getRouteListener().onRouteUnselected();
            } catch (final RemoteException ignored) {

            }
        }
    }

    public static Cursor getSingleTrackCursor(Context context, long id) {
        return getSingleTrackCursor(context, String.valueOf(id));
    }

    public static Cursor getSingleTrackCursor(Context context, String id) {
        Cursor c = context.getContentResolver().query(
                MusicProvider.RECENTS_URI,
                Projections.RECENT_SONGS,
                MusicStore.Cols._ID + "=?",
                new String[]{id}, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    @DebugLog
    public static MediaInfo buildMediaInfo(String trackTitle,
                                            String albumTitle,
                                            String artistName,
                                            String mimeType,
                                            String url,
                                            String imgUrl,
                                            String bigImageUrl) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        metadata.putString(MediaMetadata.KEY_TITLE, trackTitle);
        if (albumTitle != null) {
            metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, albumTitle);
        }
        if (artistName != null) {
            metadata.putString(MediaMetadata.KEY_ARTIST, artistName);
        }
        if (imgUrl != null) {
            metadata.addImage(new WebImage(Uri.parse(imgUrl)));
        }
        if (bigImageUrl != null) {
            metadata.addImage(new WebImage(Uri.parse(bigImageUrl)));
        }
        return new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType.startsWith("audio") ? mimeType : "audio/*") // Workaround for application/ogg (vorbis)
                .setMetadata(metadata)
                .build();
    }

    public static MediaInfo buildMediaInfo(Context context, long id) {
        MediaInfo info = null;
        Cursor c = getSingleTrackCursor(context, id);
        if (c != null) {
            info = buildMediaInfo(context, c);
            c.close();
        }
        return info;
    }

    public static MediaInfo buildMediaInfo(Context context, Cursor c) {
        if (c != null && !c.isClosed()) {
            final RecentSong song = CursorHelpers.makeRecentSongFromRecentCursor(c);
            return buildMediaInfo(context, song);
        }
        return null;
    }

    public static MediaInfo buildMediaInfo(Context context, RecentSong song) {
        if (song != null) {
            if (song.isLocal) {
                try {
                    String ipaddr = getWifiIpAddress(context);
                    return buildMediaInfo(song.name, song.albumName, song.artistName, song.mimeType,
                            buildMusicUrl(ipaddr, song.identity), buildArtUrl(ipaddr, song), null);
                } catch (UnknownHostException ignored) {}
            } else {
                String artUrl = null;
                try {
                    String ipaddr = getWifiIpAddress(context);
                    artUrl = buildArtUrl(ipaddr, song);
                } catch (UnknownHostException e) {
                    if (song.artworkUri != null) {
                        artUrl = song.artworkUri.toString();
                    }
                }
                return buildMediaInfo(song.name, song.albumName, song.artistName, song.mimeType,
                        song.dataUri.toString(), artUrl, null);
            }
        }
        return null;
    }

    public static String buildMusicUrl(long id, String host) {
        return "http://" + host + ":" + CastWebServer.PORT + "/audio/" + id;
    }

    public static String buildMusicUrl(String host, String id) {
        return "http://" + host + ":" + CastWebServer.PORT + "/audio/" + id;
    }

    public static String buildArtUrl(String host, RecentSong song) {
        String artist = song.albumArtistName != null ? song.albumArtistName : song.artistName;
        String album = song.albumName;
        if (artist == null || album == null) {
            return null;
        }
        try {
            artist = URLEncoder.encode(artist, "UTF-8");
            album = URLEncoder.encode(album, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();//better never happens
        }
        return "http://"+host+":"+CastWebServer.PORT+"/art?artist="+artist+"&album="+album;
    }

    /**
     * Returns the wifi ip addr ** only supports ipv4
     * TODO Look into Settings Utils.getWifiIpAddresses() which uses hidden methods and classes.
     */
    public static String getWifiIpAddress(Context context) throws UnknownHostException {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int hostAddress = wm.getConnectionInfo().getIpAddress();
        // from AOSP.. see NetworkUtils.intToInetAddress();
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        final String addr = InetAddress.getByAddress(addressBytes).getHostAddress();
        if ("0.0.0.0".equals(addr)) {
            throw new UnknownHostException();
        }
        return addr;
    }
}
