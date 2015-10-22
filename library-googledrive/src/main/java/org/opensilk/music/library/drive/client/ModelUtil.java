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

package org.opensilk.music.library.drive.client;

import android.net.Uri;

import com.google.api.services.drive.model.File;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.functions.Func1;

import static org.opensilk.music.library.drive.Constants.AUDIO_MIME_WILDCARD;
import static org.opensilk.music.library.drive.Constants.AUDIO_OGG_MIMETYPE;
import static org.opensilk.music.library.drive.Constants.FOLDER_MIMETYPE;
import static org.opensilk.music.library.drive.Constants.IMAGE_MIME_WILDCARD;

/**
 * Created by drew on 4/28/15.
 */
public class ModelUtil {

    public static final Func1<File, Boolean> IS_FOLDER = new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return StringUtils.equals(FOLDER_MIMETYPE, file.getMimeType());
        }
    };

    public static final Func1<File, Boolean> IS_AUDIO = new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return StringUtils.contains(file.getMimeType(), AUDIO_MIME_WILDCARD)
                    || StringUtils.equals(file.getMimeType(), AUDIO_OGG_MIMETYPE);
        }
    };

    public static final Func1<File, Boolean> IS_IMAGE = new Func1<File, Boolean>() {
        @Override
        public Boolean call(File file) {
            return StringUtils.contains(file.getMimeType(), IMAGE_MIME_WILDCARD);
        }
    };

    public static File pickSuitableImage(List<File> images) {
        if (images.size() == 1) {
            if (isAlbumArt(images.get(0).getTitle())) {
                return images.get(0);
            }
        } else {
            List<File> matches = new ArrayList<>(images.size());
            for (File image : images) {
                if (isAlbumArt(image.getTitle())) {
                    matches.add(image);
                }
            }
            if (!matches.isEmpty()) {
                int langestidx = 0;
                long largestsize = matches.get(0).getFileSize();
                for (int ii=1; ii<matches.size(); ii++) {
                    if (matches.get(ii).getFileSize() > largestsize) {
                        largestsize = matches.get(ii).getFileSize();
                        langestidx = ii;
                    }
                }
                return matches.get(langestidx);
            }
        }
        return null;
    }

    public static boolean isAlbumArt(String name) {
        return !StringUtils.containsIgnoreCase(name, "thumb");
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
