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

package org.opensilk.music.ui.cards.event;

import com.andrew.apollo.model.Genre;

/**
 * Created by drew on 6/28/14.
 */
public class GenreCardClick {

    public enum Event {
        OPEN,
        PLAY_ALL,
        SHUFFLE_ALL,
        ADD_TO_QUEUE,
        ADD_TO_PLAYLIST
    }

    public final Event event;
    public final Genre genre;

    public GenreCardClick(Event event, Genre genre) {
        this.event = event;
        this.genre = genre;
    }
}
