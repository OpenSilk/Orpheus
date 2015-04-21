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

package org.opensilk.music.iab.event;

/**
 * Created by drew on 4/26/14.
 */
public class IABQueryResult {
    public enum Error {
        NO_ERROR,
        BIND_FAILED,
        QUERY_FAILED,
        NO_SKUS
    }

    public final Error error;
    public final boolean isApproved;

    public IABQueryResult(Error error) {
        this(error, false);
    }

    public IABQueryResult(Error error, boolean isApproved) {
        this.error = error;
        this.isApproved = isApproved;
    }

    @Override
    public String toString() {
        return error.toString()+", approved="+isApproved;
    }
}
