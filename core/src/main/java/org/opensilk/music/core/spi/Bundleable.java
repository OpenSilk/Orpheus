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

package org.opensilk.music.core.spi;

import android.os.Bundle;

/**
 * Created by drew on 6/23/14.
 */
public interface Bundleable {

    /*
     * @since API_010
     */

    Bundle toBundle();
    interface BundleCreator<T> {
        public T fromBundle(Bundle b) throws IllegalArgumentException;
    }

    /*
     * @since API_020
     */

    String getIdentity();
    String getName();
}
