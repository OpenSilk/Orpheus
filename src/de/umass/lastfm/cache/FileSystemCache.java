/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm.cache;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Standard {@link Cache} implementation which is used by default by the {@link de.umass.lastfm.Caller} class.
 * This implementation caches all responses in the file system. In addition to the raw responses it stores a
 * .meta file which contains the expiration date for the specified request.
 *
 * @author Janni Kovacs
 */
@SuppressWarnings({"ALL"})
public class FileSystemCache extends Cache {

    private static final String SUBMISSIONS_FILE = "submissions.txt";

    private static final String CACHE_DIR = "/lastfm-cache";

    private File cacheDir;

    public FileSystemCache(Context context) {
        this (new File(context.getExternalCacheDir()+CACHE_DIR));
    }

    public FileSystemCache(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean contains(String cacheEntryName) {
        return new File(cacheDir, cacheEntryName + ".xml").exists();
    }

    public void remove(String cacheEntryName) {
        new File(cacheDir, cacheEntryName + ".xml").delete();
        new File(cacheDir, cacheEntryName + ".meta").delete();
    }

    public boolean isExpired(String cacheEntryName) {
        File f = new File(cacheDir, cacheEntryName + ".meta");
        if (!f.exists())
            return false;
        try {
            Properties p = new Properties();
            FileInputStream fis = new FileInputStream(f);
            p.load(fis);
            fis.close();
            long expirationDate = Long.valueOf(p.getProperty("expiration-date"));
            return expirationDate < System.currentTimeMillis();
        } catch (IOException e) {
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void clear() {
        for (File file : cacheDir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    public InputStream load(String cacheEntryName) {
        try {
            return new FileInputStream(new File(cacheDir, cacheEntryName + ".xml"));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
        createCache();
        File f = new File(cacheDir, cacheEntryName + ".xml");
        try {
            BufferedInputStream is = new BufferedInputStream(inputStream);
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
            int read;
            byte[] buffer = new byte[4096];
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.close();
            is.close();
            File fm = new File(cacheDir, cacheEntryName + ".meta");
            Properties p = new Properties();
            p.setProperty("expiration-date", Long.toString(expirationDate));
            FileOutputStream fos = new FileOutputStream(fm);
            p.store(fos, null);
            fos.close();
        } catch (IOException e) {
            // we ignore the exception. if something went wrong we just don't cache it.
        }
    }

    private void createCache() {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            if (!cacheDir.isDirectory()) {
                this.cacheDir = cacheDir.getParentFile();
            }
        }
    }

}
