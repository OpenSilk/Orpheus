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

package org.opensilk.music.ui2.event;

import android.content.Intent;

/**
 * Created by drew on 10/7/14.
 */
public class StartActivityForResult {
    public static final int PLUGIN_REQUEST_LIBRARY = 1001;
    public static final int PLUGIN_REQUEST_SETTINGS = 1002;
    public static final int APP_REQUEST_SETTINGS = 1003;

    public final Intent intent;
    public final int reqCode;

    public StartActivityForResult(Intent intent, int reqCode) {
        this.intent = intent;
        this.reqCode = reqCode;
    }

}
