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

package org.opensilk.util.coverartarchive;

import android.content.Context;
import android.util.Log;

import com.andrew.apollo.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

/**
 * Created by drew on 3/2/14.
 */
public class CoverArtFetcher {

    private static final String TAG = CoverArtFetcher.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    private static final String API_ROOT = "http://coverartarchive.org/release/";
    private static final String FRONT_COVER_URL = API_ROOT+"%s/front";

    public static final String CACHE_DIR = "/downloaded-art/";

    private static final String USER_AGENT = "CoverArtFetcher";

    private Context mContext;

    private static CoverArtFetcher sInstance;

    public static CoverArtFetcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CoverArtFetcher(context);
        }
        return sInstance;
    }

    private CoverArtFetcher(Context context) {
        mContext = context;
    }

    public String getFrontCoverUrl(String mbid) {
        String urlString = String.format(Locale.US, FRONT_COVER_URL, mbid);
        if (D) Log.i(TAG, "Checking " + urlString);
        String coverUrl = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(false);
            connection.connect();
//            if (D) {
//                for (Map.Entry<String, List<String>> header: connection.getHeaderFields().entrySet()) {
//                    Log.i(TAG, header.getKey() +"="+header.getValue());
//                }
//            }
            if (connection.getResponseCode() == 307) { //Apparently HttpUrlConnection doest have a 307 constant
                coverUrl = connection.getHeaderField("Location");
                if (D) Log.i(TAG, "Found art at: " + coverUrl);
            } else {
                if (D) Log.w(TAG, "connection returned " + connection.getResponseMessage());
            }
            connection.disconnect();
        } catch (MalformedURLException e) {
            Log.e(TAG, ""+e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, ""+e.getMessage());
        }
        return coverUrl;
    }

    public File downloadCover(String urlString, String fileName) {
        File outFile = new File(mContext.getExternalCacheDir()+CACHE_DIR+fileName);
        ReadableByteChannel in = null;
        FileChannel out = null;
        try {
            // Parse the url (Do this first so we don't have to delete file on error)
            URL url = new URL(urlString);
            // Create parent dir
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            // create file
            outFile.createNewFile();
            // open url
            in = Channels.newChannel(url.openStream());
            // open our file channel
            out = new FileOutputStream(outFile).getChannel();
            // write the file (Use a loop instead of transferFrom() so we don't need to get the size)
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytes;
            do {
                bytes = in.read(buffer);
                buffer.flip();
                out.write(buffer);
                buffer.compact();
            } while (bytes >= 0 || buffer.position() > 0);
        } catch (MalformedURLException e) {
            Log.e(TAG, "downloadCover(2)"+e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "downloadCover(3)"+e.getMessage());
            if (outFile.exists()) {
                outFile.delete();
            }
            return null;
        } finally {
            if (in != null && in.isOpen()) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null && out.isOpen()) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
        return outFile;
    }

}
