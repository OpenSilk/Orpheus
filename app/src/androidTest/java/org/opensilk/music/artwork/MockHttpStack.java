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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.umass.lastfm.opensilk.Fetch;

/**
 * Created by drew on 10/22/14.
 */
public class MockHttpStack implements HttpStack {

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> stringStringMap) throws IOException, AuthFailureError {
        HttpResponse hr_ok = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        String url = request.getUrl();
        if (url.startsWith(CoverArtJsonRequest.API_ROOT)) {
            if (url.contains(ArtworkRequestManagerTest.TEST_MBID1)) {
                HttpEntity he = new StringEntity(readResource("raw/caa_mbidresp.json"), "UTF-8");
                hr_ok.setEntity(he);
                return hr_ok;
            }
        } else if (url.startsWith(Fetch.DEFAULT_API_ROOT)) {
            if (url.contains(Fetch.PARAM_METHOD_ALBUM_INFO)) {
                HttpEntity he = new StringEntity(readResource("raw/lfm_albumresp.xml"), "UTF-8");
                hr_ok.setEntity(he);
                return hr_ok;
            } else if (url.contains(Fetch.PARAM_METHOD_ARTIST_INFO)) {
                HttpEntity he = new StringEntity(readResource("raw/lfm_artistresp.xml"), "UTF-8");
                hr_ok.setEntity(he);
                return hr_ok;
            }
        }
        return null;
    }

    String readResource(String name) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        String res = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        return res;
    }

}
