/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.plugin.drive.util;

import android.os.Bundle;
import android.os.RemoteException;

import org.opensilk.music.api.callback.Result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Created by drew on 11/11/14.
 */
@Singleton
public class RequestCache {

    final Map<String, List<Bundle>> CACHE = Collections.synchronizedMap(new LinkedHashMap<String, List<Bundle>>());

    @Inject
    public RequestCache() {

    }

    public void put(String cacheKey, List<Bundle> bundles) {
        CACHE.put(cacheKey, bundles);
    }

    public boolean get(String cacheKey, int startpos, int maxResults, Result callback) {
        synchronized (CACHE) {
            if (CACHE.containsKey(cacheKey)) {
                Timber.d("get() hit=%s", cacheKey);
                List<Bundle> list = CACHE.get(cacheKey);
                int start = startpos < list.size() ? startpos : list.size();
                int end = startpos+maxResults < list.size() ? startpos+maxResults : list.size();
                Timber.d("get() cachesize=%d, start=%d, end=%d, startPos=%d maxResults=%d",
                        list.size(), start, end, startpos, maxResults);
                final List<Bundle> results;
                if (start < end) {
                    results = list.subList(start, end);
                } else {
                    results = Collections.emptyList();
                }
                final Bundle token;
                if (end < list.size()) {
                    token = new Bundle(1);
                    token.putInt("startpos", end);
                } else {
                    token = null;
                }
                try {
                    callback.onNext(results, token);
                } catch (RemoteException ignored) {}
                return true;
            }
            return false;
        }
    }

    public void clear() {
        CACHE.clear();
    }

}
