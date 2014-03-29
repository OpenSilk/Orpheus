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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v7.media.MediaRouter;

import com.andrew.apollo.MusicPlaybackService;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.opensilk.cast.ICastService;
import org.opensilk.cast.manager.BaseCastManager;
import org.opensilk.cast.manager.ReconnectionStatus;
import org.opensilk.cast.util.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 2/13/14.
 */
public class CastUtils {

    private CastUtils() {
        //static
    }

    public static ICastService sCastService;

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
                if (sCastService.getCastManager().getReconnectionStatus() == ReconnectionStatus.FINALIZE) {
                    sCastService.getCastManager().setReconnectionStatus(ReconnectionStatus.INACTIVE);
                    return true;
                }
                Utils.saveStringToPreference(context, BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
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
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MusicPlaybackService.PROJECTION,
                "_id=" + id,
                null, null);
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
        metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, albumTitle);
        metadata.putString(MediaMetadata.KEY_ARTIST, artistName);
        metadata.addImage(new WebImage(Uri.parse(imgUrl)));
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
            try {
                final String ipAddr = getWifiIpAddress(context);
                return buildMediaInfo(
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)),
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)),
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)),
                        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)),
                        buildMusicUrl(c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID)),
                                ipAddr),
                        buildArtUrl(c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)),
                                ipAddr),
                        null
                );
            } catch (UnknownHostException|IllegalArgumentException ignored) {
                // fall
            }
        }
        return null;
    }

    public static String buildMusicUrl(long id, String host) {
        return "http://" + host + ":" + CastWebServer.PORT + "/audio/" + id;
    }

    public static String buildArtUrl(long id, String host) {
        return "http://" + host + ":" + CastWebServer.PORT + "/art/" + id;
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
