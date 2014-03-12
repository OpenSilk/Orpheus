package com.andrew.apollo.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import de.umass.lastfm.cache.Cache;

/**
 * Created by drew on 3/11/14.
 */
public class LastFMRequestCache extends Cache {

    private Context mContext;

    private static LastFMRequestCache sInstance;

    public static synchronized LastFMRequestCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LastFMRequestCache(context);
        }
        return sInstance;
    }

    public LastFMRequestCache(Context context) {
        mContext = context;
    }

    @Override
    public synchronized boolean contains(String cacheEntryName) {
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(MusicProvider.LFMREQ_URI,
                    new String[] { "id" }, "id=?", new String[] { cacheEntryName }, null);
            if (c != null && c.getCount() > 0) {
                return true;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    @Override
    public synchronized InputStream load(String cacheEntryName) {
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(MusicProvider.LFMREQ_URI,
                    new String[]{ "response" }, "id=?", new String[]{ cacheEntryName }, null);
            if (c != null && c.moveToFirst()) {
                String response = c.getString(0);
                if (response != null) {
                    return new ByteArrayInputStream(response.getBytes("UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException ignored) {

        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    @Override
    public synchronized void remove(String cacheEntryName) {
        mContext.getContentResolver().delete(MusicProvider.LFMREQ_URI,
                "id=?", new String[]{cacheEntryName});
    }

    @Override
    public synchronized void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
        // http://stackoverflow.com/a/5445161/472621
        Scanner s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        String response = (s != null && s.hasNext()) ? s.next() : "";
        if (TextUtils.isEmpty(response)) {
            return;
        }
        ContentValues values = new ContentValues(4);
        values.put("id", cacheEntryName);
        values.put("expiration_date", expirationDate);
        values.put("response", response);
        mContext.getContentResolver().insert(MusicProvider.LFMREQ_URI, values);
    }

    @Override
    public synchronized boolean isExpired(String cacheEntryName) {
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(MusicProvider.LFMREQ_URI,
                    new String[]{ "expiration_date" }, "id=?", new String[]{ cacheEntryName }, null);
            if (c != null && c.moveToFirst()) {
                long expiration = c.getLong(0);
                return expiration < System.currentTimeMillis();
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        mContext.getContentResolver().delete(MusicProvider.LFMREQ_URI, "1", null);
    }

}
