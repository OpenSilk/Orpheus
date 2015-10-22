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

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.util.Preconditions;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 *
 */
public final class OkHttpTransport extends HttpTransport {

    /**
     * All valid request methods as specified in {@link HttpURLConnection#setRequestMethod}, sorted in
     * ascending alphabetical order.
     */
    private static final String[] SUPPORTED_METHODS = {HttpMethods.DELETE,
            HttpMethods.GET,
            HttpMethods.HEAD,
            HttpMethods.OPTIONS,
            HttpMethods.POST,
            HttpMethods.PUT,
            HttpMethods.TRACE};
    static {
        Arrays.sort(SUPPORTED_METHODS);
    }

    private final OkHttpClient okClient;

    public OkHttpTransport(OkHttpClient okClient) {
        this.okClient = okClient;
    }

    @Override
    public boolean supportsMethod(String method) {
        return Arrays.binarySearch(SUPPORTED_METHODS, method) >= 0;
    }

    @Override
    protected LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        Preconditions.checkArgument(supportsMethod(method), "HTTP method %s not supported", method);
        URL connUrl = new URL(url);
        Request.Builder rob = new Request.Builder().url(connUrl);
        return new OkLowLevelHttpRequest(okClient.clone(), method, rob);
    }

}
