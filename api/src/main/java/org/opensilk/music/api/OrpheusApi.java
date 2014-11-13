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
     * Manifest permission declared by Orpheus.
     */
    public static final String PERMISSION_BIND_LIBRARY_SERVICE = "org.opensilk.music.api.permission.BIND_LIBRARY_SERVICE";
    /**
     * Intent extra containing {@link String} library identity, used in multiple places
     */
    public static final String EXTRA_LIBRARY_ID = "org.opensilk.music.api.LIBRARY_ID";
    /**
     * Intent extra containing the {@link org.opensilk.music.api.meta.LibraryInfo}
     */
    public static final String EXTRA_LIBRARY_INFO = "org.opensilk.music.api.LIBRARY_INFO";
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
     * Api version 0.2.0, used internally
     */
    public static final int API_020 = 20000;

    @Deprecated
    public static class Ability {
        public static final int SEARCH = 1 << 0;
        public static final int SETTINGS = 1 << 1;
    }

    @Deprecated
    public static class Error {
        public static final int UNKNOWN = -1;
        public static final int RETRY = 1;
        public static final int AUTH_FAILURE = 2;
        public static final int NETWORK = 3;
    }

    /**
     * Transforms Bundles passed to Orpheus into a {@link org.opensilk.music.api.model.spi.Bundleable} object
     *
     * @param b Bundle created with {@link org.opensilk.music.api.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.api.model.spi.Bundleable}
     * @throws java.lang.Exception if bundle is malformed
     */
    @NonNull
    public static Bundleable materializeBundle(Bundle b) throws Exception {
        Class cls = Class.forName(b.getString("clz"));
        return materializeBundle(cls, b);
    }

    /**
     * Transforms Bundles passed to Orpheus into their real class
     *
     * @param cls Class this bundle is created from
     * @param b Bundle created with {@link org.opensilk.music.api.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.api.model.spi.Bundleable}
     * @throws java.lang.Exception if passed {@link android.os.Bundle} does not implement
     *          {@link org.opensilk.music.api.model.spi.Bundleable}
     */
    @NonNull @SuppressWarnings("unchecked")
    public static <T extends Bundleable> T materializeBundle(Class<T> cls, Bundle b) throws Exception {
        Field f = cls.getDeclaredField("BUNDLE_CREATOR");
        Bundleable.BundleCreator<T> creator = (Bundleable.BundleCreator<T>) f.get(null);
        return creator.fromBundle(b);
    }

}
