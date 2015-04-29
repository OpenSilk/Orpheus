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

import android.net.Uri;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by drew on 4/28/15.
 */
public class ModelUtil {
    public static Folder buildFolder(File f) {
        final String id = f.getId();
        final String title = f.getTitle();
        List<ParentReference> parents = f.getParents();
        final String parentId = parents.size() > 0 ? parents.get(0).getId() : null;
        final String date = formatDate(f.getModifiedDate().getValue());
        return new Folder.Builder()
                .setIdentity(id)
                .setName(title)
                .setParentIdentity(parentId)
                .setDate(date)
                .build();
    }

    public static Track buildTrack(File f, String authToken) {
        final String id = f.getId();
        final String title = f.getTitle();
        final String mimeType = f.getMimeType();
        final Uri data = Uri.parse(f.getDownloadUrl());// buildDownloadUri(f.getDownloadUrl(), authToken);
        return new Track.Builder()
                .setIdentity(id)
                .setName(title)
                .setDataUri(data)
                .setMimeType(mimeType)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();
    }

    public static Uri buildDownloadUri(String url, String authToken) {
        return Uri.parse(buildDownloadUriString(url, authToken));
    }

    public static String buildDownloadUriString(String url, String authToken) {
        return String.format(Locale.US, "%s&access_token=%s", url, authToken);
    }

    public static String formatDate(long ms) {
        Date date = new Date(ms);
        DateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return out.format(date);
    }
}
