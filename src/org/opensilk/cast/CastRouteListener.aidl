package org.opensilk.cast;

/**
 * Created by drew on 2/20/14.
 */
interface CastRouteListener {
    void onRouteSelected(in Bundle castDevice);
    void onRouteUnselected();
}
