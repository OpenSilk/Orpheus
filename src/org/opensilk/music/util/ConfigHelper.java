package org.opensilk.music.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;

/**
 * Created by andrew on 3/18/14.
 */
public class ConfigHelper {

    public static final int SPLITSCREEN_THRESHOLD = 800; //dp

    public static boolean isPortrait(Resources res) {
        return res.getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    public static boolean isLargeScreen(Resources res) {
        return (res.getConfiguration().screenLayout & SCREENLAYOUT_SIZE_MASK) >= SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isLargeLandscape(Resources res) {
        DisplayMetrics metrics = res.getDisplayMetrics();
        return !isPortrait(res)
                && (metrics.widthPixels / metrics.scaledDensity) >= SPLITSCREEN_THRESHOLD;
    }
}
