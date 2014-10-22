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

package org.opensilk.music.artwork;

import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by drew on 10/21/14.
 */
class CoverArtJsonRequest extends JsonRequest<String> {
    private static final String API_ROOT = "http://coverartarchive.org/release/";

    interface Listener extends Response.Listener<String>, Response.ErrorListener {

    }

    static class CoverArtResponse {
        List<Image> images;
        String release;
        static class Image {
            List<String> types;
            boolean front;
            boolean back;
            String comment;
            String image;
            Thumbnail thumbnails;
            boolean approved;
            int edit;
        }
        static class Thumbnail {
            String small;
            String large;
        }
    }

    final Listener listener;
    final Gson gson;

    CoverArtJsonRequest(String mbid, Listener listener, Gson gson) {
        super(Request.Method.GET, makeUrl(mbid), null, listener, listener);
        this.listener = listener;
        this.gson = gson;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            // gson is so fucking awesome
            CoverArtResponse car = gson.fromJson(jsonString, CoverArtResponse.class);
            for (CoverArtResponse.Image image : car.images) {
                if (image.front && !TextUtils.isEmpty(image.image)) {
                    return Response.success(image.image, HttpHeaderParser.parseCacheHeaders(response));
                }
            }
        } catch (Exception ignored) {
            //fall
        }
        return Response.error(new VolleyError("Unable to process json"));
    }

    private static String makeUrl(String mbid) {
        return API_ROOT+mbid;
    }
}
