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

package org.opensilk.music.plugin.drive;

/**
 * Created by drew on 4/28/15.
 */
public interface Constants {
    String DEFAULT_ROOT_FOLDER = "root";
    String BASE_QUERY = " in parents and trashed=false ";
    String FOLDER_MIMETYPE = "application/vnd.google-apps.folder";
    String AUDIO_MIME_WILDCARD = "audio";
    String AUDIO_OGG_MIMETYPE = "application/ogg";
    String FOLDER_SONG_QUERY = " (mimeType='"+FOLDER_MIMETYPE+"' or mimeType contains '"
            +AUDIO_MIME_WILDCARD+"' or mimeType='"+AUDIO_OGG_MIMETYPE+"')";
    String SONG_QUERY = " (mimeType contains '"+AUDIO_MIME_WILDCARD+"' or mimeType='"
            +AUDIO_OGG_MIMETYPE+"')";
    String FIELDS = "items/id,items/mimeType,items/parents,items/title,items/downloadUrl,items/modifiedDate";
}
