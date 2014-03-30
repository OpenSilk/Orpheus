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

package org.opensilk.music.artwork;

import android.graphics.Bitmap;

import com.android.volley.Response;

import java.util.Locale;

/**
 * Created by drew on 3/29/14.
 */
public class CoverArtArchiveRequest extends ArtworkImageRequest {
    private static final String API_ROOT = "http://coverartarchive.org/release/";
    private static final String FRONT_COVER_URL = API_ROOT+"%s/front";

    public CoverArtArchiveRequest(String mbid, Response.Listener<Bitmap> listener,
                                  ArtworkType imageType, Response.ErrorListener errorListener) {
        super(makeUrl(mbid), listener, imageType, errorListener);
    }

    private static String makeUrl(String mbid) {
        return String.format(Locale.US, FRONT_COVER_URL, mbid);
    }
}
