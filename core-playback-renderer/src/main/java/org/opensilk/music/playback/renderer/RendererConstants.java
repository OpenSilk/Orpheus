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

package org.opensilk.music.playback.renderer;

/**
 * Created by drew on 11/1/15.
 */
public interface RendererConstants {
    /**
     * Intent extra passed by Orpheus to plugin activities to help them determine whether to use light or dark themes
     */
    String EXTRA_WANT_LIGHT_THEME = "org.opensilk.music.library.extra.WANT_LIGHT_THEME";

    /**
     * Name of metadata on service to indicate class of picker activity if there are multiple
     * devices available for playback
     */
    String META_PICKER_ACTIVITY = "org.opensilk.music.renderer.META_PICKER_ACTIVITY";

    /**
     * Intent filter action indicating this service is an orpheus compatible renderer
     */
    String ACTION_RENDERER_SERVICE = "org.opensilk.music.renderer.action.RENDERER_SERVICE";
}
