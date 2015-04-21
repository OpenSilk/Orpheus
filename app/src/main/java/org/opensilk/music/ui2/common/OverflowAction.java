/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.common;

import org.opensilk.music.R;

/**
 * Created by drew on 10/27/14.
 */
public enum OverflowAction {
    //        OPEN(-1),
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

    private OverflowAction(int resourceId) {
        this.resourceId = resourceId;
    }

    public static OverflowAction valueOf(int resourceId) {
        for (OverflowAction a : OverflowAction.values()) {
            if (a.resourceId == resourceId) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown id: "+ resourceId);
    }
}
