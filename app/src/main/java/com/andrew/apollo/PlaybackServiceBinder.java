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

package com.andrew.apollo;

import android.media.session.MediaSession;
import android.os.RemoteException;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 4/22/15.
 */
//@Singleton
public class PlaybackServiceBinder extends IPlaybackService.Stub {
    private final WeakReference<PlaybackService> mService;

    @Inject
    public PlaybackServiceBinder(PlaybackService mService) {
        this.mService = new WeakReference<PlaybackService>(mService);
    }

    @Override
    public MediaSession.Token getToken() throws RemoteException {
        PlaybackService service = mService.get();
        if (service != null) {
            return service.getToken();
        }
        return null;
    }
}
