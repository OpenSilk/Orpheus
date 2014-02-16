package org.opensilk.music.cast;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.cache.ImageCache;
import com.andrew.apollo.cache.ImageFetcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 2/14/14.
 * Based on SimpleWebServer from NanoHttpd
 *
 * Serves audio files and album art
 * url requests must be in the form:
 *      /audio/${id}
 *      /art/${id}
 * where id is value of audio._id of the track in the MediaStore
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

    public CastWebServer(Context context) {
        this(context, CastUtils.getWifiIpAddress(context), PORT);
    }

    public CastWebServer(Context context, String host, int port) {
        super(host, port);
        mContext = context;
        // Initialize the image fetcher
        mImageFetcher = ImageFetcher.getInstance(context);
        // Initialize the image cache
        mImageFetcher.setImageCache(ImageCache.getInstance(context));
    }

    @DebugLog
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

    @DebugLog
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
    private static final String MIME_ART = "image/webp";

    /**
     * Fetches and serves the album art
     *
     * @param uri
     * @param headers
     * @return
     */
    @DebugLog
    private Response serveArt(String uri, Map<String, String> headers) {
        Response res;
        String id = parseId(uri);
        Cursor c = CastUtils.getSingleTrackCursor(mContext, id);
        if (c == null) {
            return notFoundResponse();
        }
        // get the cached artwork
        // TODO play with ImageCache and create method to return raw InputStream
        final Bitmap bitmap = mImageFetcher.getArtwork(getAlbumName(c), getAlbumId(c), getArtistName(c));
        c.close();
        // Convert the bitmap into a bytearray
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, os);
        // form and check etag
        String etag = Integer.toHexString(os.hashCode());
        // build response
        if (etag.equals(headers.get("if-none-match"))) {
            res = createResponse(Response.Status.NOT_MODIFIED, MIME_ART, "");
        } else {
            res = createResponse(Response.Status.OK, MIME_ART, new ByteArrayInputStream(os.toByteArray()));
            res.addHeader("ETag", etag);
        }
        return res;

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
        /* See @SimpleWebServer#serveFile
        * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias
        */
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
