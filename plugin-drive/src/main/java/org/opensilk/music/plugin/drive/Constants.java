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

import com.google.api.services.drive.model.File;

import org.apache.commons.lang3.StringUtils;

import rx.functions.Func1;

/**
 * Created by drew on 4/28/15.
 */
public interface Constants {

    String DEFAULT_ROOT_FOLDER = "root";

    String FOLDER_MIMETYPE = "application/vnd.google-apps.folder";
    String AUDIO_MIME_WILDCARD = "audio";
    String AUDIO_OGG_MIMETYPE = "application/ogg";

    String BASE_FOLDERS_TRACKS_QUERY = " in parents and trashed=false and " +
            "(mimeType='" + FOLDER_MIMETYPE  + "' or mimeType contains '"
            + AUDIO_MIME_WILDCARD + "' or mimeType='" + AUDIO_OGG_MIMETYPE + "')";

    String TRACKS_QUERY = "trashed=false and " +
            "(mimeType contains '" + AUDIO_MIME_WILDCARD + "' or mimeType='" + AUDIO_OGG_MIMETYPE + "')";

    String LIST_FIELDS = "items/id,items/mimeType,items/parents,items/title,items/downloadUrl,items/modifiedDate";

    Func1<File, Boolean> IS_FOLDER = new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return StringUtils.equals(FOLDER_MIMETYPE, file.getMimeType());
        }
    };

    Func1<File, Boolean> IS_AUDIO = new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return StringUtils.contains(file.getMimeType(), AUDIO_MIME_WILDCARD)
                    || StringUtils.equals(file.getMimeType(), AUDIO_OGG_MIMETYPE);
        }
    };
}
