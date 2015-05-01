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

package org.opensilk.music.artwork;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;

import javax.inject.Inject;

/**
 * Created by drew on 4/30/15.
 */
public class ArtworkService extends Service {

    @Inject BitmapDiskCache mDiskCache;
    @Inject BitmapLruCache mL2Cache;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
