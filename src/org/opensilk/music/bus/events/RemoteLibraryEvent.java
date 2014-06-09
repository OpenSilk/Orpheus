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

package org.opensilk.music.bus.events;

import android.content.ComponentName;

/**
 * Created by drew on 6/14/14.
 */
public class RemoteLibraryEvent {

    private RemoteLibraryEvent() {

    }

    public static class Bound {
        public final ComponentName componentName;
        public Bound(ComponentName componentName) {
            this.componentName = componentName;
        }
    }

    public static class Unbound {
        public final ComponentName componentName;
        public Unbound(ComponentName componentName) {
            this.componentName = componentName;
        }
    }

}
