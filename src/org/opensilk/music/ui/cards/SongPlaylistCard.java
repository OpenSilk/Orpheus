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

import android.content.Context;
import android.widget.PopupMenu;

import com.andrew.apollo.R;

import org.opensilk.music.api.model.Song;

/**
 * Created by drew on 6/30/14.
 */
public class SongPlaylistCard extends SongCard {

    public SongPlaylistCard(Context context, Song song) {
        super(context, song);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        super.onCreatePopupMenu(m);
        m.getMenu().removeItem(R.id.popup_delete);
    }
}
