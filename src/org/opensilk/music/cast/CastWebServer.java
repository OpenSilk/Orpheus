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
package org.opensilk.music.cast;

import android.content.Context;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.android.volley.toolbox.ByteArrayPool;
import com.android.volley.toolbox.PoolingByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.artwork.ArtworkProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import hugo.weaving.DebugLog;

/**
 * Based on SimpleWebServer from NanoHttpd
 *
 * Serves audio files and album art
 * url requests must be in the form:
 *      /audio/${audio._id}
 *      /art?artist={name}&album={name}
 *
 * Created by drew on 2/14/14.
 */
public class CastWebServer extends NanoHTTPD {
    private static final String TAG = CastWebServer.class.getSimpleName();

    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    public static final String MIME_DEFAULT_AUDIO = "audio/*";

    public static final int PORT = 50989;

    private static final String[] TRACK_PROJECTION;

    static {
        TRACK_PROJECTION = new String[] {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
        };
    }

    static class TrackInfo {
        String path;
        String mime;
    }

    private final boolean quiet = !BuildConfig.DEBUG;
    private final Context mContext;
    private final WifiManager.WifiLock mWifiLock;
    private final LruCache<String, String> mEtagCache;
    private final ByteArrayPool mBytePool;

    public CastWebServer(Context context) throws UnknownHostException {
        this(context, CastUtils.getWifiIpAddress(context), PORT);
    }

    public CastWebServer(Context context, String host, int port) {
        super(host, port);
        mContext = context;
        // get the lock
        mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "CastServer");
        // arbitrary size might increase as needed;
        mEtagCache = new LruCache<>(20);
        mBytePool = new ByteArrayPool(2*1024*1024);
    }

    @Override
    public void start() throws IOException {
        super.start();
        mWifiLock.acquire();
    }

    @Override
    public void stop() {
        mWifiLock.release();
        super.stop();
    }

    public Response serve(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        if (!quiet) {
            Log.v(TAG, session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                Log.v(TAG, "  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                Log.v(TAG, "  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        return respond(Collections.unmodifiableMap(header), Collections.unmodifiableMap(parms), uri);
    }

    private Response respond(Map<String, String> headers, Map<String, String> params, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
        }

        Response response = null;

        if (uri.startsWith("/audio")) {
            response = serveSong(uri, headers);
        } else if (uri.startsWith("/art")) {
            response = serveArt(headers, params, uri);
        }

        return response != null ? response : notFoundResponse();
    }

    /* Change if needed */
    private static final String MIME_ART = "image/*";

    /**
     * Fetches and serves the album art
     *
     * @param uri
     * @param headers
     * @return
     */
    //@DebugLog
    private Response serveArt(Map<String, String> headers, Map<String, String> params, String uri) {
        String artist = params.get("artist");
        String album= params.get("album");
        if (TextUtils.isEmpty(artist) || TextUtils.isEmpty(album)) {
            return notFoundResponse();
        }
        String reqEtag = headers.get("if-none-match");
        if (!quiet) Log.d(TAG, "requested Art etag " + reqEtag);
        // Check our cache if we served up this etag for this url already
        // we can just return and save ourselfs a lot of expensive db/disk queries
        if (!TextUtils.isEmpty(reqEtag)) {
            String oldUri;
            synchronized (mEtagCache) {
                oldUri = mEtagCache.get(reqEtag);
            }
            if (oldUri != null && oldUri.equals(uri)) {
                // We already served it
                return createResponse(Response.Status.NOT_MODIFIED, MIME_ART, "");
            }
        }
        // We've got get get the art
        InputStream parcelIn = null;
        ByteArrayOutputStream tmpOut = null;
        try {
            final ParcelFileDescriptor pfd = mContext.getContentResolver()
                    .openFileDescriptor(ArtworkProvider.createArtworkUri(artist, album), "r");
            //Hackish but hopefully will yield unique etags (at least for this session)
            String etag = Integer.toHexString(pfd.hashCode());
            if (!quiet) Log.d(TAG, "Created etag " + etag + " for " + uri);
            synchronized (mEtagCache) {
                mEtagCache.put(etag, uri);
            }
            // pipes dont perform well over the network and tend to get broken
            // so copy the image into memory and send the copy
            parcelIn = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            tmpOut = new PoolingByteArrayOutputStream(mBytePool, 512*1024);
            IOUtils.copy(parcelIn, tmpOut);
            if (!quiet) Log.d(TAG, "image size=" + tmpOut.size()/1024.0 + "k");
            Response res = createResponse(Response.Status.OK, MIME_ART, new ByteArrayInputStream(tmpOut.toByteArray()));
            res.addHeader("ETag", etag);
            return res;
        } catch (NullPointerException|IOException e) {
            // Serve up the default art
            return createResponse(Response.Status.OK, MIME_ART,mContext.getResources().openRawResource(R.drawable.default_artwork));
        } finally {
            IOUtils.closeQuietly(tmpOut);
            IOUtils.closeQuietly(parcelIn);
        }
    }

    /**
     * Locates and serves the audio track
     *
     * @param uri
     * @param headers
     * @return
     */
    //@DebugLog
    private Response serveSong(String uri, Map<String,String> headers) {
        String id = parseId(uri);
        if (TextUtils.isEmpty(id)) {
            return notFoundResponse();
        }
        TrackInfo info = getTrackInfo(mContext, id);
        if (info == null) {
            return notFoundResponse();
        }
        if (TextUtils.isEmpty(info.path)) {
            return notFoundResponse();
        }
        if (TextUtils.isEmpty(info.mime) || !info.mime.startsWith("audio")) {
            info.mime = MIME_DEFAULT_AUDIO;
        }
        return serveFile(new File(info.path), info.mime, headers);
    }

    /* See @SimpleWebServer#serveFile
     * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias
     */
    //@DebugLog
    private Response serveFile(File file, String mime, Map<String, String> headers) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = headers.get("range");
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

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(headers.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }

    /**
     * @param uri
     * @return track id from url
     */
    private static String parseId(String uri) {
        int start = uri.lastIndexOf("/");
        return uri.substring(start+1);
    }

    private static String[] parseArtUri(String uri) {
        String[] parts = uri.split("/");
        String[] info = new String[2];
        if (parts.length < 2) {
            return null;
        }
        info[1] = parts[parts.length-1];
        info[0] = parts[parts.length-2];
        return info;
    }

    private static TrackInfo getTrackInfo(final Context context, final String id) {
        Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                TRACK_PROJECTION,
                "audio._id=?",
                new String[] {id}, null);
        if (c == null) {
            return null;
        }
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        try {
            TrackInfo info = new TrackInfo();
            info.path = getPath(c);
            info.mime = getMimeType(c);
            return info;
        } catch (Exception e) {
            return null;
        } finally {
            c.close();
        }
    }

    /**
     * @return The path to the current song
     */
    public static String getPath(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA));
    }

    /**
     * @return The current sond Mime Type
     */
    public static String getMimeType(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE));
    }

    private static Response notFoundResponse() {
        return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }

    // Announce that the file server accepts partial content requests
    private static Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private static Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

}
