package org.opensilk.cast;

import org.opensilk.cast.CastRouteListener;
/**
 * Created by drew on 2/19/14.
 */
interface ICastManager {
    void changeVolume(double increment);
    int getReconnectionStatus();
    void setReconnectionStatus(int status);
    CastRouteListener getRouteListener();
}
