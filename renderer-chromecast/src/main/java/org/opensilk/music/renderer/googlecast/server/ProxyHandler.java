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

package org.opensilk.music.renderer.googlecast.server;

import android.net.Uri;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opensilk.music.model.Track;
import org.opensilk.music.renderer.googlecast.TrackResCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;

/**
 * Handles requests that require auth by proxying them through
 * the device (which has been previously authenticated)
 *
 * Note that this in not a true proxy, we merely make the request on their
 * behalf and feed them the input stream.
 *
 * Created by drew on 10/30/15.
 */
public class ProxyHandler extends AbstractHandler {
    final OkHttpClient mOkClient;
    final TrackResCache mTrackResCache;

    @Inject
    public ProxyHandler(
            OkHttpClient mOkClient,
            TrackResCache mTrackResCache
    ) {
        this.mOkClient = mOkClient;
        this.mTrackResCache = mTrackResCache;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!HttpMethods.GET.equals(request.getMethod())) {
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED_405);
            baseRequest.setHandled(true);
            return;
        }

        String pathInfo = StringUtils.stripStart(request.getPathInfo(), "/");
        Uri contentUri = Uri.parse(CastServerUtil.decodeString(pathInfo));

        if (CastServer.DUMP_REQUEST_HEADERS) {
            StringBuilder reqlog = new StringBuilder();
            reqlog.append("Serving artwork uri ").append(contentUri).append("\n Method ").append(request.getMethod());
            for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
                String name = names.nextElement();
                reqlog.append("\n HDR: ").append(name).append(":").append(request.getHeader(name));
            }
            Timber.v(reqlog.toString());
        }

        com.squareup.okhttp.Request.Builder rb = new com.squareup.okhttp.Request.Builder()
                .url(contentUri.toString());
        Track.Res trackRes = mTrackResCache.get(contentUri);

        //add resource headers
        Map<String, String> headers = trackRes.getHeaders();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            rb.addHeader(e.getKey(), e.getValue());
        }

        String range = request.getHeader("Range");
        if (!StringUtils.isEmpty(range)) {
            rb.addHeader("Range", range);
        }

        String ifnonematch = request.getHeader("if-none-match");
        if (!StringUtils.isEmpty(ifnonematch)) {
            rb.addHeader("if-none-match", ifnonematch);
        }

        //dont clog our cache with binaries
        CacheControl pCC = new CacheControl.Builder().noStore().noCache().build();
        rb.cacheControl(pCC);

        Response pResponse = mOkClient.newCall(rb.get().build()).execute();

        if (CastServer.DUMP_REQUEST_HEADERS) {
            StringBuilder sb = new StringBuilder();
            sb.append("Executed proxy GET request uri ").append(contentUri)
                    .append("\n Resp: ").append(pResponse.code())
                    .append(",").append(pResponse.message());
            for (String name : pResponse.headers().names()) {
                sb.append("\n HDR: ").append(name).append(": ").append(pResponse.header(name));
            }
            Timber.v(sb.toString());
        }

        if (!pResponse.isSuccessful()) {
            response.sendError(pResponse.code(), pResponse.message());
            baseRequest.setHandled(true);
            return;
        }

        //build the response
        String acceptRanges = pResponse.header("Accept-Ranges");
        if (!StringUtils.isEmpty(acceptRanges)) {
            response.addHeader("Accept-Ranges", acceptRanges);
        }
        String contentRange = pResponse.header("Content-Range");
        if (!StringUtils.isEmpty(contentRange)) {
            response.addHeader("Content-Range", contentRange);
        }
        String contentLen = pResponse.header("Content-Length");
        if (!StringUtils.isEmpty(contentLen)) {
            response.addHeader("Content-Length", contentLen);
        }
        String contentType = pResponse.header("Content-Type");
        if (StringUtils.isEmpty(contentType)) {
            contentType = "application/octet-stream";
        }
        response.addHeader("Content-Type", contentType);
        String etag = pResponse.header("Etag");
        if (!StringUtils.isEmpty(etag)) {
            response.addHeader("Etag", etag);
        }
        if (HttpStatus.NOT_MODIFIED_304 == pResponse.code()) {
            response.flushBuffer();
        } else {
            InputStream in = pResponse.body().byteStream();
            try {
                //XXX out need not be closed
                OutputStream out = response.getOutputStream();
                IOUtils.copy(in, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
        baseRequest.setHandled(true);
    }
}
