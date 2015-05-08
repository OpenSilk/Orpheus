/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.opensilk.music.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Various navigation helpers.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class NavUtils {

    /**
     * Alias wrapper after renaming home activity
     */
    public static Intent makeLauncherIntent(Context context) {
        return new Intent()
                .setComponent(new ComponentName(context, "org.opensilk.music.ui.activities.HomeSlidingActivity"))
                .setAction("org.opensilk.music.AUDIO_PLAYER")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

}
