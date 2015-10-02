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

package org.opensilk.bundleable;

import android.os.Bundle;

/**
 * Like Parcelable but not. A Bundleable object can be
 * transformed into a bundle by calling its {@link #toBundle()}
 * method. And transformed back into itself by calling
 * {@link org.opensilk.bundleable.Bundleable.BundleCreator#fromBundle(Bundle)}.
 * <br/>
 * In order to be compatible with {@link BundleableUtil#materializeBundle(Bundle)}
 * The {@link org.opensilk.bundleable.Bundleable.BundleCreator} must be defined
 * in a public static field named BUNDLE_CREATOR. In addition, the Bundle returned
 * by {@link #toBundle()} must have a string value with the fully qualified class name
 * under the key {@link #CLZ}
 * <br/>
 * Why use this? Parcelable is great and all but bundles are too. Bundles can be
 * passed between processes, without worrying about type safety or (as long
 * as the Bundleable only contains primitives and platform objects) setting
 * the proper classLoader.
 * <br/>
 * This originally came into existence to more easily pass lists of mixed objects
 * across ipc in a single call. see {@link BundleableListSlice}
 *
 * Created by drew on 6/23/14.
 */
public interface Bundleable {
    String CLZ = "clz";

    Bundle toBundle();
    interface BundleCreator<T> {
        T fromBundle(Bundle b) throws IllegalArgumentException;
    }

}
