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

import org.opensilk.music.R;

/**
 * Created by drew on 7/16/14.
 */
public enum CardEvent {
    OPEN(-1),
    PLAY_NEXT(R.id.popup_play_next),
    PLAY_ALL(R.id.popup_play_all),
    SHUFFLE_ALL(R.id.popup_shuffle_all),
    ADD_TO_QUEUE(R.id.popup_add_to_queue),
    ADD_TO_PLAYLIST(R.id.popup_add_to_playlist),
    SET_RINGTONE(R.id.popup_set_ringtone),
    MORE_BY_ARTIST(R.id.popup_more_by_artist),
    RENAME(R.id.popup_rename),
    DELETE(R.id.popup_delete);

    private final int resourceId;

    private CardEvent(int resourceId) {
        this.resourceId = resourceId;
    }

    public static CardEvent valueOf(int resourceId) {
        for (CardEvent e: CardEvent.values()) {
            if (e.resourceId == resourceId) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown id: "+ resourceId);
    }
}
