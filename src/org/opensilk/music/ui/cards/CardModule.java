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

package org.opensilk.music.ui.cards;

import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.ArtistCardClickHandler;
import org.opensilk.music.ui.cards.handler.GenreCardClickHandler;
import org.opensilk.music.ui.cards.handler.PlaylistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongGroupCardClickHandler;

import dagger.Module;

/**
 * Created by drew on 6/24/14.
 */
@Module (
        injects = {
                AlbumCard.class,
                ArtistCard.class,
                FileItemCard.class,
                FolderCard.class,
                GenreCard.class,
                PlaylistCard.class,
                SongAlbumCard.class,
                SongCard.class,
                SongGroupCard.class,
                SongPlaylistCard.class,
                SongQueueCard.class,
                AlbumCardClickHandler.class,
                ArtistCardClickHandler.class,
                GenreCardClickHandler.class,
                PlaylistCardClickHandler.class,
                SongCardClickHandler.class,
                SongGroupCardClickHandler.class,
        },
        complete = false
)
public class CardModule {
}
