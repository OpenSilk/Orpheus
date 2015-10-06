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

import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 4/30/15.
 */
public interface Constants {
    /** Largest size of artwork */
    int MAX_ARTWORK_SIZE_DP = 720;
    /** Largest size of any thumbnail displayed */
    int DEFAULT_THUMBNAIL_SIZE_DP = 200;

    float THUMB_MEM_CACHE_DIVIDER = 0.15f;
    String DISK_CACHE_DIRECTORY = "artworkcache";

    Scheduler ARTWORK_SCHEDULER = Schedulers.computation();
}
