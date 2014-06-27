/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apollo.model;

import org.opensilk.music.api.model.Song;

/**
 * Created by drew on 6/26/14.
 */
public class RecentSong {

    public final Song song;
    public final long id;
    public final boolean isLocal;
    public final int playcount;
    public final long lastplayed;

    public RecentSong(Song song, long id, boolean isLocal, int playcount, long lastplayed) {
        this.song = song;
        this.id = id;
        this.isLocal = isLocal;
        this.playcount = playcount;
        this.lastplayed = lastplayed;
    }

}
