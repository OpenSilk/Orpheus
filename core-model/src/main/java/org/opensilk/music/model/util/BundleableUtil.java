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

package org.opensilk.music.model.util;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.music.model.ex.BadBundleableException;
import org.opensilk.music.model.spi.Bundleable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by drew on 4/25/15.
 */
public class BundleableUtil {

    /**
     * Transforms Bundles passed to Orpheus into a {@link org.opensilk.music.model.spi.Bundleable} object
     *
     * @param b Bundle created with {@link org.opensilk.music.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.model.spi.Bundleable}
     * @throws BadBundleableException if bundle is malformed
     */
    @NonNull
    public static <T extends Bundleable> T materializeBundle(Bundle b) throws BadBundleableException {
        return materializeBundle(b.getString("clz"), b);
    }

    /**
     * Transforms Bundles passed to Orpheus into their real class
     *
     * @param cls Class this bundle is created from
     * @param b Bundle created with {@link org.opensilk.music.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.model.spi.Bundleable}
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
     * @param b Bundle created with {@link org.opensilk.music.model.spi.Bundleable#toBundle()}
     * @return {@link org.opensilk.music.model.spi.Bundleable}
     * @throws BadBundleableException if bundle is malformed
     */
    @NonNull @SuppressWarnings("unchecked")
    public static <T extends Bundleable> T materializeBundle(String name, Bundle b) throws BadBundleableException {
        try {
            return (T) getCreator(name, b).fromBundle(b);
        } catch (IllegalArgumentException|ClassCastException e) {
            throw new BadBundleableException(e);
        }
    }

    //Laziness need to just make everything parcelable.
    public static void flatten(@NonNull Bundle outState, @NonNull List<Bundleable> bundleables) {
        ArrayList<ParcelableBundleable> pbs = new ArrayList<>(bundleables.size());
        for (Bundleable b : bundleables) {
            pbs.add(new ParcelableBundleable(b));
        }
        outState.putParcelableArrayList("wrapped", pbs);
    }

    public static List<Bundleable> unflatten(@NonNull Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("wrapped")) {
            ArrayList<ParcelableBundleable> pbs = savedInstanceState.getParcelableArrayList("wrapped");
            if (pbs != null) {
                ArrayList<Bundleable> bundleables = new ArrayList<>(pbs.size());
                for (ParcelableBundleable pb : pbs) {
                    bundleables.add(pb.bundleable);
                }
                return bundleables;
            }
        }
        return Collections.emptyList();
    }

    private static final HashMap<String, Bundleable.BundleCreator> sCreatorCache = new HashMap<>();
    private static Bundleable.BundleCreator getCreator(String name, Bundle b) throws BadBundleableException {
        if (name == null) {
            throw new BadBundleableException(new NullPointerException("clz not found in bundle"));
        }
        Bundleable.BundleCreator creator;
        synchronized (sCreatorCache) {
            creator = sCreatorCache.get(name);
            if (creator == null) {
                try {
                    Class clz = Class.forName(b.getString("clz"));
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
