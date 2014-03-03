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
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
 *      /art/${album._id}
 *
 * We use the album id for art so our etags will work effectively
 *
 * Created by drew on 2/14/14.
 */
public class CastWebServer extends NanoHTTPD {
    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    public static final int PORT = 8080;

    private static final String TAG = CastWebServer.class.getSimpleName();

    /**
     * The columns used to retrieve any info from the current track
     */
    private static final String[] PROJECTION = new String[] {
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };

    private final boolean quiet = !BuildConfig.DEBUG;
    private final Context mContext;
    private final ImageFetcher mImageFetcher;
    private final WifiManager.WifiLock mWifiLock;
    private final LruCache<String, String> mEtagCache;

    public CastWebServer(Context context) {
        this(context, CastUtils.getWifiIpAddress(context), PORT);
    }

    public CastWebServer(Context context, String host, int port) {
        super(host, port);
        mContext = context;
        // Initialize the image fetcher
        mImageFetcher = ImageFetcher.getInstance(context);
        mImageFetcher.setImageCache(ImageCache.getInstance(context));
        // get the lock
        mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "CastServer");
        // arbitrary size might increase as needed;
        mEtagCache = new LruCache<String, String>(20);
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

        return respond(Collections.unmodifiableMap(header), uri);
    }

    private Response respond(Map<String, String> headers, String uri) {
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
            response = serveArt(uri, headers);
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
    @DebugLog
    private Response serveArt(String uri, Map<String, String> headers) {
        String id = parseId(uri);
        String reqEtag = headers.get("if-none-match");
        Log.d(TAG, "requested Art etag " + reqEtag);
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
        Album album = MusicUtils.makeAlbum(mContext, Integer.valueOf(id));
        if (album == null) {
            return notFoundResponse();
        }
        File file = null;
        synchronized (mImageFetcher) {
            file = mImageFetcher.getLargeArtworkFile(album.mAlbumName, album.mAlbumId, album.mArtistName);
        }
        if (file != null) {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());
            // Cache the art etag
            synchronized (mEtagCache) {
                mEtagCache.put(etag, uri);
            }
            return serveFile(file, MIME_ART, headers);
        }
        return notFoundResponse();
    }

    /**
     * Locates and serves the audio track
     *
     * @param uri
     * @param headers
     * @return
     */
    @DebugLog
    private Response serveSong(String uri, Map<String,String> headers) {
        Response res;
        String id = parseId(uri);
        Cursor c = CastUtils.getSingleTrackCursor(mContext, id);
        if (c == null) {
            return notFoundResponse();
        }
        String filePath = getPath(c);
        String mime = getMimeType(c);
        c.close();
        if (TextUtils.isEmpty(filePath)) {
            return notFoundResponse();
        }
        if (TextUtils.isEmpty(mime)) {
            mime = MIME_DEFAULT_BINARY;
        }
        File file = new File(filePath);
        return serveFile(file, mime, headers);
    }

    /* See @SimpleWebServer#serveFile
     * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias
     */
    @DebugLog
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
    private String parseId(String uri) {
        int start = uri.lastIndexOf("/");
        return uri.substring(start+1);
    }

    /**
     * @return The path to the current song
     */
    public static String getPath(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA));
    }

    /**
     * @return The current song album Name
     */
    public static String getAlbumName(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
    }

    /**
     * @return The current song name
     */
    public static String getTrackName(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE));
    }

    /**
     * @return The current song artist name
     */
    public static String getArtistName(Cursor c) {
        return c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
    }

    /**
     * @return The current song album ID
     */
    public static long getAlbumId(Cursor c) {
        return c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
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
