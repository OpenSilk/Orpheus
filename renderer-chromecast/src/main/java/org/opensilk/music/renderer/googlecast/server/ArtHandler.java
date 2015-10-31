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

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.util.LruCache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opensilk.music.renderer.googlecast.CastRendererService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;

/**
 * Created by drew on 10/30/15.
 */
public class ArtHandler extends AbstractHandler {

    final CastRendererService mService;
    final LruCache<String, Uri> mEtagCache;

    @Inject
    public ArtHandler(CastRendererService mService) {
        this.mService = mService;
        this.mEtagCache = new LruCache<>(100);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pathInfo = StringUtils.stripStart(request.getPathInfo(), "/");
        Uri contentUri = Uri.parse(CastServerUtil.decodeString(pathInfo));
        StringBuilder reqlog = new StringBuilder();
        reqlog.append("Serving artwork uri ").append(contentUri).append("\n Method ").append(request.getMethod());
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            reqlog.append("\n HDR: ").append(name).append(":").append(request.getHeader(name));
        }
        Timber.v(reqlog.toString());
        String etag = request.getHeader("if-none-match");
        if (!StringUtils.isEmpty(etag)) {
            if (contentUri.equals(mEtagCache.get(etag))) {
                Timber.d("Already served artwork %s etag=%s", contentUri, etag);
                response.setStatus(HttpStatus.NOT_MODIFIED_304);
                response.flushBuffer();
                baseRequest.setHandled(true);
                return;
            }
        }
        ParcelFileDescriptor pfd = mService.getAccessor().getArtwork(contentUri);
        if (pfd == null) {
            response.sendError(HttpStatus.NOT_FOUND_404);
            baseRequest.setHandled(true);
            return;
        }
        response.setStatus(HttpStatus.OK_200);
        //will yield unique etags for this session
        etag = UUID.randomUUID().toString();
        mEtagCache.put(etag, contentUri);
        response.setHeader("Etag", etag);
        response.setContentType("image/*");
        response.setContentLength((int)pfd.getStatSize());
        AssetFileDescriptor afd = new AssetFileDescriptor(pfd, 0, pfd.getStatSize());
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
