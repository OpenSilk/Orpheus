package org.opensilk.cast;

import android.content.Context;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.Config;

/**
 * Created by drew on 2/19/14.
 */
public class CastManagerFactory {
    private static CastManager sCastManager = null;

    private CastManagerFactory() {
    }

    public static CastManager getCastManager(Context context) {
        if (sCastManager == null) {
            sCastManager = CastManager.initialize(context, Config.CAST_APPLICATION_ID, null, null);
            if (BuildConfig.DEBUG) {
                sCastManager.enableFeatures(CastManager.FEATURE_DEBUGGING);
            }
            // We are streaming /from/ the device so it needs to exit
            sCastManager.setStopOnDisconnect(true);
        }
        return sCastManager;
    }
}
