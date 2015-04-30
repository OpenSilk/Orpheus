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

package org.opensilk.music.library.provider;

/**
 * Out of band stuffs
 *
 * Created by drew on 4/29/15.
 */
public interface OB {
    /**
     * Methods
     */
    interface M {
        /**
         * Request plugin config
         */
        String LIBRARYCONF = "m_libraryconf";
        /**
         * Called after receiving a null cursor
         * arg = Uri string
         */
        String ONNULLCURSOR = "m_onnullcursor";
    }

    /**
     * Response Bundle keys
     */
    interface K {
        /**
         * ParcelalbeException
         */
        String EX = "k_ex";
    }
}
