package org.opensilk.music.util;

import android.content.res.Resources;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;

/**
 * Created by andrew on 3/18/14.
 */
public class ConfigHelper {

    public static boolean isPortrait(Resources res) {
        return res.getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    public static boolean isLargeScreen(Resources res) {
        return (res.getConfiguration().screenLayout & SCREENLAYOUT_SIZE_MASK) >= SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isLargeLandscape(Resources res) {
        return isLargeScreen(res) && !isPortrait(res);
    }
}
