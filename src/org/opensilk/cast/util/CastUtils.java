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
package org.opensilk.cast.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.MediaStore;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.Config;
import com.andrew.apollo.MusicPlaybackService;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.opensilk.cast.CastManager;

import org.opensilk.music.cast.CastWebServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by drew on 2/13/14.
 */
public class CastUtils {

    private CastUtils() {
        //static
    }

    public static CastManager sCastMgr = null;

    public static CastManager getCastManager(Context context) {
        if (null == sCastMgr) {
            sCastMgr = CastManager.initialize(context, Config.CAST_APPLICATION_ID, null, null);
            if (BuildConfig.DEBUG) {
                sCastMgr.enableFeatures(CastManager.FEATURE_DEBUGGING);
            }
            // We are streaming /from/ the device so it needs to exit
            sCastMgr.setStopOnDisconnect(true);
        }
        sCastMgr.setContext(context);

        return sCastMgr;
    }

    public static Cursor getSingleTrackCursor(Context context, long id) {
        return getSingleTrackCursor(context, String.valueOf(id));
    }

    public static Cursor getSingleTrackCursor(Context context, String id) {
        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MusicPlaybackService.PROJECTION,
                "_id=" + id,
                null, null, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    public static MediaInfo buildMediaInfo(String trackTitle,
                                            String albumTitle,
                                            String artistName,
                                            String mimeType,
                                            String url,
                                            String imgUrl,
                                            String bigImageUrl) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        metadata.putString(MediaMetadata.KEY_TITLE, trackTitle);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, albumTitle);
        metadata.putString(MediaMetadata.KEY_STUDIO, artistName);
        metadata.addImage(new WebImage(Uri.parse(imgUrl)));
        if (bigImageUrl != null) {
            metadata.addImage(new WebImage(Uri.parse(bigImageUrl)));
        }
        return new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType)
                .setMetadata(metadata)
                .build();
    }

    public static MediaInfo buildMediaInfo(Context context, long id) {
        Cursor c = getSingleTrackCursor(context, id);
        if (c != null) {
            MediaInfo info = buildMediaInfo(
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)),
                    buildMusicUrl(id, getWifiIpAddress(context)),
                    buildArtUrl(id, getWifiIpAddress(context)),
                    null
            );
            c.close();
            return info;
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
    public static String getWifiIpAddress(Context context) {
        String addr = "0.0.0.0";
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int hostAddress = wm.getConnectionInfo().getIpAddress();
        // from AOSP.. see NetworkUtils.intToInetAddress();
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        try {
            addr = InetAddress.getByAddress(addressBytes).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return addr;
    }
}
