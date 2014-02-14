package org.opensilk.music.cast;

import android.content.Context;

import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.utils.Utils;

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
}
