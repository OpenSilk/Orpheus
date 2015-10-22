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

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by drew on 10/22/15.
 */
public class OkLowLevelHttpRequest extends LowLevelHttpRequest {
    private final OkHttpClient okClient;
    private final String method;
    private final Request.Builder builder;

    public OkLowLevelHttpRequest(OkHttpClient okClient, String method, Request.Builder builder) {
        this.okClient = okClient;
        this.method = method;
        this.builder = builder;
    }

    @Override
    public void addHeader(String name, String value) throws IOException {
        builder.addHeader(name, value);
    }

    @Override
    public void setTimeout(int connectTimeout, int readTimeout) throws IOException {
        okClient.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        okClient.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public LowLevelHttpResponse execute() throws IOException {
        if (getStreamingContent() != null) {
            String contentType = getContentType();
            String contentEncoding = getContentEncoding();
            if (contentEncoding != null) {
                addHeader("Content-Encoding", contentEncoding);
            }
            long contentLength = getContentLength();
            //TODO handle upload properly
            ByteArrayOutputStream out = new ByteArrayOutputStream(contentLength > 0 ? (int) contentLength : 1024);
            try {
                getStreamingContent().writeTo(out);
                MediaType mediaType = MediaType.parse(contentType);
                RequestBody body = RequestBody.create(mediaType, out.toByteArray());
                builder.method(method, body);
            } finally {
                out.close();
            }
        } else {
            builder.method(method, null);
        }
        Response response = okClient.newCall(builder.build()).execute();
        return new OkLowLevelHttpResponse(response);
    }
}
