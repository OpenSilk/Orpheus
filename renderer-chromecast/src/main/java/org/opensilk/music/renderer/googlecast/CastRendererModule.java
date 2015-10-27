/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.renderer.googlecast;

import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import org.opensilk.common.core.dagger2.ForApplication;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/30/15.
 */
@Module
public class CastRendererModule {

    final CastRendererService service;

    public CastRendererModule(CastRendererService service) {
        this.service = service;
    }

    @Provides
    CastRendererService provideCastService() {
        return service;
    }

    //TODO these are declared here temporarily to avoid conflicts with PlaybackComponent

    @Provides
    ConnectivityManager provideConnectivityManager(@ForApplication Context service) {
        return (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    WifiManager provideWifimanager(@ForApplication Context service) {
        return (WifiManager) service.getSystemService(Context.WIFI_SERVICE);
    }

    @Provides
    AudioManager provideAudioManager(@ForApplication Context service) {
        return (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }
}
