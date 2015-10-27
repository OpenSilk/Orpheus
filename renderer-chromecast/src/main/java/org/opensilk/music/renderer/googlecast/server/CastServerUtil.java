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
package org.opensilk.music.renderer.googlecast.server;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Base64;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Track;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * Created by drew on 2/13/14.
 */
public class CastServerUtil {

    public CastServerUtil() {
        //static
    }

    public static boolean verifyTrackResForEmulator(Track.Res res) {
        return !IS_EMULATOR || StringUtils.isEmpty(res.getHeaderString())
                && StringUtils.startsWith(res.getUri().getScheme(), "http");
    }

    public static MediaInfo makeMediaInfo(Track track, MediaMetadataCompat mediaMetadataCompat, String baseUrl) {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.getName());
        String albumName = track.getAlbumName();
        if (!StringUtils.isEmpty(albumName)) {
            mediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE, albumName);
        }
        String albumArtistName = track.getAlbumArtistName();
        if (!StringUtils.isEmpty(albumArtistName)) {
            mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, albumArtistName);
        }
        String artistName = track.getArtistName();
        if (!StringUtils.isEmpty(artistName)) {
            mediaMetadata.putString(MediaMetadata.KEY_ARTIST, artistName);
        }
        int trackNumber = track.getTrackNumber();
        if (trackNumber > 0) {
            mediaMetadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, track.getTrackNumber());
        }
        if (!IS_EMULATOR) {
            String artworkUriString = mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            if (artworkUriString != null) {
                mediaMetadata.addImage(new WebImage(buildArtUri(baseUrl, artworkUriString)));
            }
        }
        Track.Res trackRes = track.getResources().get(0);
        String contentId;
        Uri resUri = trackRes.getUri();
        if (StringUtils.startsWith(resUri.toString(), "http")) {
            if (StringUtils.isEmpty(trackRes.getHeaderString())) {
                contentId = resUri.toString(); //No auth;
            } else {
                contentId = buildProxyTrackUrl(baseUrl, resUri);
            }
        } else {
            contentId = buildLocalTrackUrl(baseUrl, resUri);
        }
        String mimeType = trackRes.getMimeType();
        return new MediaInfo.Builder(contentId)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(StringUtils.startsWith(mimeType, "audio") ? mimeType : "audio/*")
                .setMetadata(mediaMetadata)
                .build();
    }

    static Uri buildArtUri(String baseUrl, String contentUriString) {
        return Uri.parse(baseUrl).buildUpon().appendPath("artwork")
                .appendPath(encodeString(contentUriString)).build();
    }

    static String buildLocalTrackUrl(String baseUrl, Uri contentUri) {
        return Uri.parse(baseUrl).buildUpon().appendPath("track").appendPath("local")
                .appendPath(encodeString(contentUri.toString())).build().toString();
    }

    static String buildProxyTrackUrl(String baseUrl, Uri contentUri) {
        return Uri.parse(baseUrl).buildUpon().appendPath("track").appendPath("proxy")
                .appendPath(encodeString(contentUri.toString())).build().toString();
    }

    public static String encodeString(String string) {
        return Base64.encodeToString(string.getBytes(Charset.defaultCharset()),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static String decodeString(String string) {
        return new String(Base64.decode(string, Base64.URL_SAFE), Charset.defaultCharset());
    }

    /**
     * Returns the wifi ip addr ** only supports ipv4
     * TODO Look into Settings Utils.getWifiIpAddresses() which uses hidden methods and classes.
     */
    public static String getWifiIpAddress(WifiManager wm) throws UnknownHostException {
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

    public static final boolean IS_EMULATOR;
    static {
        IS_EMULATOR = StringUtils.contains(Build.FINGERPRINT, "sdk");
    }
}
