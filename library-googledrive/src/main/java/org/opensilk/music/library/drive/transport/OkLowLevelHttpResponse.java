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

package org.opensilk.music.library.drive.transport;

import com.google.api.client.http.LowLevelHttpResponse;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by drew on 10/22/15.
 */
public class OkLowLevelHttpResponse extends LowLevelHttpResponse {
    private final Response okResponse;

    public OkLowLevelHttpResponse(Response okResponse) {
        this.okResponse = okResponse;
    }

    @Override
    public InputStream getContent() throws IOException {
        return okResponse.body().byteStream();
    }

    @Override
    public String getContentEncoding() throws IOException {
        return okResponse.header("Content-Encoding");
    }

    @Override
    public long getContentLength() throws IOException {
        return okResponse.body().contentLength();
    }

    @Override
    public String getContentType() throws IOException {
        return okResponse.body().contentType().toString();
    }

    @Override
    public String getStatusLine() throws IOException {
        String result = okResponse.headers().value(0);
        return result != null && result.startsWith("HTTP/1.") ? result : null;
    }

    @Override
    public int getStatusCode() throws IOException {
        return okResponse.code();
    }

    @Override
    public String getReasonPhrase() throws IOException {
        return okResponse.message();
    }

    @Override
    public int getHeaderCount() throws IOException {
        return okResponse.headers().size();
    }

    @Override
    public String getHeaderName(int index) throws IOException {
        return okResponse.headers().name(index);
    }

    @Override
    public String getHeaderValue(int index) throws IOException {
        return okResponse.headers().value(index);
    }
}
