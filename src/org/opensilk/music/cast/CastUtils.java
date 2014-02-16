package org.opensilk.music.cast;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.Config;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by drew on 2/13/14.
 */
public class CastUtils {

    private CastUtils() {
        //static
    }

    public static VideoCastManager sCastMgr = null;

    public static VideoCastManager getCastManager(Context context) {
        if (null == sCastMgr) {
            sCastMgr = VideoCastManager.initialize(context, Config.CAST_APPLICATION_ID, null, null);
            if (BuildConfig.DEBUG) {
                sCastMgr.enableFeatures(VideoCastManager.FEATURE_DEBUGGING);
            }
            // We are streaming /from/ the device so it needs to exit
            sCastMgr.setStopOnDisconnect(true);
        }
        sCastMgr.setContext(context);

        return sCastMgr;
    }

    /**
     * Returns the wifi ip addr ** only supports ipv4
     * TODO ethernet, ipv6
     * Look into Settings Utils.getWifiIpAddresses() which uses hidden methods and classes.
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

    public static String buildMusicUrl(long id, String host) {
        return "http://" + host + ":" + CastWebServer.PORT + "/audio/" + id;
    }

    public static String buildArtUrl(long id, String host) {
        return "http://" + host + ":" + CastWebServer.PORT + "/art/" + id;
    }

    public static MediaInfo buildSample(Context context) {
        String host = getWifiIpAddress(context);

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

            movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Ivy Levan");
            movieMetadata.putString(MediaMetadata.KEY_TITLE, "Hang Forever");
//            movieMetadata.putString(MediaMetadata.KEY_STUDIO, studio);
            movieMetadata.addImage(new WebImage(Uri.parse("http://" + host + ":8080/art/14696")));
//            movieMetadata.addImage(new WebImage(Uri.parse(bigImageUrl)));

            return new MediaInfo.Builder("http://" + host + ":8080/audio/14696")
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setContentType("audio/mpeg")
                    .setMetadata(movieMetadata)
                    .build();
    }
}
