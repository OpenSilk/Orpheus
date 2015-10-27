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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opensilk.common.core.dagger2.ForApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;

/**
 * Created by drew on 10/30/15.
 */
public class LocalHandler extends AbstractHandler {
    final ContentResolver mContentResolver;

    @Inject
    public LocalHandler(@ForApplication Context mContext) {
        this.mContentResolver = mContext.getContentResolver();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pathInfo = StringUtils.stripStart(request.getPathInfo(), "/");
        Uri contentUri = Uri.parse(CastServerUtil.decodeString(pathInfo));
        StringBuilder reqlog = new StringBuilder();
        reqlog.append("Serving local uri ").append(contentUri).append("\n Method ").append(request.getMethod());
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            reqlog.append("\n HDR: ").append(name).append(":").append(request.getHeader(name));
        }
        Timber.v(reqlog.toString());
        AssetFileDescriptor afd = mContentResolver.openAssetFileDescriptor(contentUri, "r");
        if (afd == null) {
            Timber.e("Invalid uri");
            response.sendError(HttpStatus.NOT_FOUND_404);
            baseRequest.setHandled(true);
            return;
        }

        String mime = mContentResolver.getType(contentUri);

        // Support (simple) skipping:
        long startFrom = 0;
        long endAt = -1;
        String range = request.getHeader("range");
        if (range != null) {
            if (range.startsWith("bytes=")) {
                range = range.substring("bytes=".length());
                int minus = range.indexOf('-');
                try {
                    if (minus > 0) {
                        startFrom = Long.parseLong(range.substring(0, minus));
                        endAt = Long.parseLong(range.substring(minus + 1));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        long fileLen = afd.getLength();
        if (range != null && startFrom >= 0) {
            if (startFrom >= fileLen) {
                response.addHeader("Content-Range", "bytes 0-0/"+fileLen);
                response.sendError(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE_416);
                baseRequest.setHandled(true);
                return;
            }
            if (endAt < 0) {
                endAt = fileLen - 1;
            }
            long newLen = endAt - startFrom + 1;
            if (newLen < 0) {
                newLen = 0;
            }
            //create a new truncated afd
            afd = new AssetFileDescriptor(afd.getParcelFileDescriptor(), startFrom, newLen);
            response.setStatus(HttpStatus.PARTIAL_CONTENT_206);
            response.setContentType(StringUtils.isEmpty(mime) ? "application/octet-stream" : mime);
            response.addHeader("Content-Length", "" + newLen);
            response.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
            Timber.d("Sending partial content %s\n bytes %d-%s/%d Len=%d", contentUri, startFrom, endAt, fileLen, newLen);
        } else {
            response.setStatus(HttpStatus.OK_200);
            response.setContentType(StringUtils.isEmpty(mime) ? "application/octet-stream" : mime);
            response.addHeader("Content-Length", "" + fileLen);
        }
        response.addHeader("Accept-Ranges", "bytes");
        if (HttpMethods.HEAD.equals(request.getMethod())) {
            response.flushBuffer();
            afd.close();
            baseRequest.setHandled(true);
        } else {
            InputStream in = afd.createInputStream();
            OutputStream out = response.getOutputStream();
            try {
                IOUtils.copy(in, out);
                out.flush();
                baseRequest.setHandled(true);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
}
