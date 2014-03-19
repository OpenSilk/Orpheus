package org.opensilk.music.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * Created by andrew on 3/18/14.
 */
public class ConfigHelper {
    public static boolean isPortrait(Resources res) {
        return (res.getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) ? true : false;
    }

    public static boolean isTablet(Resources res) {
        boolean xlarge = ((res.getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((res.getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }
}
