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

package org.opensilk.bundleable;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by drew on 4/25/15.
 */
public class BundleableUtil {

    /**
     * Transforms Bundles passed to Orpheus into a {@link Bundleable} object
     *
     * @param b Bundle created with {@link Bundleable#toBundle()}
     * @return {@link Bundleable}
     * @throws BadBundleableException if bundle is malformed
     */
    @NonNull
    public static <T extends Bundleable> T materializeBundle(Bundle b) throws BadBundleableException {
        return materializeBundle(b.getString(Bundleable.CLZ), b);
    }

    /**
     * Transforms Bundles passed to Orpheus into their real class
     *
     * @param cls Class this bundle is created from
     * @param b Bundle created with {@link Bundleable#toBundle()}
     * @return {@link Bundleable}
     * @throws BadBundleableException if bundle is malformed
     */
    @NonNull @SuppressWarnings("unchecked")
    public static <T extends Bundleable> T materializeBundle(Class<T> cls, Bundle b) throws BadBundleableException {
        return materializeBundle(cls.getName(), b);
    }

    /**
     * Transforms Bundles passed to Orpheus into their real class
     *
     * @param name Class name this bundle is created from
     * @param b Bundle created with {@link Bundleable#toBundle()}
     * @return {@link Bundleable}
     * @throws BadBundleableException if bundle is malformed
     */
    @NonNull @SuppressWarnings("unchecked")
    public static <T extends Bundleable> T materializeBundle(String name, Bundle b) throws BadBundleableException {
        try {
            return (T) getCreator(name).fromBundle(b);
        } catch (IllegalArgumentException|ClassCastException e) {
            sCreatorCache.remove(name);
            throw new BadBundleableException(e);
        }
    }

    private static final HashMap<String, Bundleable.BundleCreator> sCreatorCache = new HashMap<>();
    private static Bundleable.BundleCreator getCreator(String name) throws BadBundleableException {
        if (name == null) {
            throw new BadBundleableException(new NullPointerException("clz not found in bundle"));
        }
        Bundleable.BundleCreator creator;
        synchronized (sCreatorCache) {
            creator = sCreatorCache.get(name);
            if (creator == null) {
                try {
                    Class clz = Class.forName(name);
                    Field f = clz.getDeclaredField("BUNDLE_CREATOR");
                    creator = (Bundleable.BundleCreator)f.get(null);
                } catch (Exception e) {
                    throw new BadBundleableException(e);
                }
                sCreatorCache.put(name, creator);
            }
        }
        return creator;
    }
}
