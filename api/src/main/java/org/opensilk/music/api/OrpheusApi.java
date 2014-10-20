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

package org.opensilk.music.api;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.music.api.model.spi.Bundleable;

import java.lang.reflect.Field;

/**
 * Created by drew on 6/14/14.
 */
public class OrpheusApi {

    /**
     * Intent action Orpheus queries for to discover plugins
     */
    public static final String ACTION_LIBRARY_SERVICE = "org.opensilk.music.plugin.LIBRARY_SERVICE";
    /**
     * Intent extra containing library identity, used in multiple places
     */
    public static final String EXTRA_LIBRARY_ID = "org.opensilk.music.api.LIBRARY_ID";
    /**
     * Intent extra passed by Orpheus to plugin activities to help them determine whether to use light or dark themes
     */
    public static final String EXTRA_WANT_LIGHT_THEME = "org.opensilk.music.api.pick.WANT_LIGHT_THEME";

    /**
     * Current api version, used internally
     */
    public static final int API_VERSION = BuildConfig.VERSION_CODE;
    /**
     * Api version 0.1.0, used internally
     */
    public static final int API_010 = 10000;

    /**
     * Bits used in {@link RemoteLibraryService#getCapabilities()}
     */
    public static class Ability {
        /**
         * Plugin implements {@link RemoteLibraryService#search(String, String, int, android.os.Bundle, org.opensilk.music.api.callback.Result) search()}
         */
        public static final int SEARCH = 1 << 0;
        /**
         * Plugin implements {@link RemoteLibraryService#getSettingsIntent()}
         */
        public static final int SETTINGS = 1 << 1;
    }

    /**
     * Error codes used in {@link org.opensilk.music.api.callback.Result#failure(int, String)}
     */
    public static class Error {
        /**
         * Permanent failure, Orpheus will attempt to rebind service
         */
        public static final int UNKNOWN = -1;
        /**
         * Temporary failure, Orpheus will retry request immediately (up to 4 times)
         */
        public static final int RETRY = 1;
        /**
         * Permanent auth failure, Orpheus will relaunch the library picker activity
         */
        public static final int AUTH_FAILURE = 2;
        /**
         * IO or Network error, Orpheus will check connectivity and retry (up to 4 times)
         */
        public static final int NETWORK = 3;
    }

    /**
     * Transforms Bundles passed to Orpheus into a {@link org.opensilk.music.api.model.spi.Bundleable} object
     *
     * @param b Bundle created with {@link org.opensilk.music.api.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.api.model.spi.Bundleable} or null if Bundle is malformed
     * @throws java.lang.Exception
     */
    @NonNull
    public static Bundleable transformBundle(Bundle b) throws Exception {
        Class cls = Class.forName(b.getString("clz"));
        return transformBundle(cls, b);
    }

    /**
     * Transforms Bundles passed to Orpheus into their real class
     *
     * @param cls Class this bundle is created from
     * @param b Bundle created with {@link org.opensilk.music.api.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.api.model.spi.Bundleable} object or null if passed Bundle
     *          does not implement {@link org.opensilk.music.api.model.spi.Bundleable}
     * @throws java.lang.Exception
     */
    @NonNull
    public static <T extends Bundleable> T transformBundle(Class<T> cls, Bundle b) throws Exception {
        // TODO better way?
        Field f = cls.getDeclaredField("BUNDLE_CREATOR");
        Bundleable.BundleCreator<T> creator = (Bundleable.BundleCreator<T>) f.get(null);
        return creator.fromBundle(b);
    }
}
