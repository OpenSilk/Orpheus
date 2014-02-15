package org.opensilk.music.cast;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.utils.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by drew on 2/13/14.
 */
public class CastUtils {

    public static VideoCastManager sCastMgr = null;

    public static VideoCastManager getCastManager(Context context) {
        if (null == sCastMgr) {
            sCastMgr = VideoCastManager.initialize(context, "25A19AAC", null, null);
            sCastMgr.enableFeatures(
                    VideoCastManager.FEATURE_NOTIFICATION |
                            VideoCastManager.FEATURE_LOCKSCREEN |
                            VideoCastManager.FEATURE_DEBUGGING);

        }
        sCastMgr.setContext(context);
        sCastMgr.setStopOnDisconnect(true);
        return sCastMgr;
    }

    /**
     * Returns the wifi ip addr ** only supports ipv4
     * TODO ethernet, ipv6
     * Look into Settings Utils.getWifiIpAddresses() which uses hidden methods and classes.
     */
    public static String getWifiIpAddress(Context context) throws UnknownHostException {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int hostAddress = wm.getConnectionInfo().getIpAddress();
        // from AOSP.. see NetworkUtils.intToInetAddress();
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        return InetAddress.getByAddress(addressBytes).getHostAddress();
    }

    public static MediaInfo buildSample(Context context) {
        String host;
        try {
            host = getWifiIpAddress(context);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }

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
