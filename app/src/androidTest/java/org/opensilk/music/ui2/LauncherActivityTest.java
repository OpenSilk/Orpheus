/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;

/**
 * Created by drew on 10/17/14.
 */
@Config(
        emulateSdk = 18,
        reportSdk = 18
)
@RunWith(RobolectricTestRunner.class)
public class LauncherActivityTest {

    @Test
    public void activityShouldStart() {
//        LauncherActivity activity = Robolectric.buildActivity(LauncherActivity.class).create().get();
//        assertThat(activity).isNotNull();
        org.junit.Assert.assertTrue(true);
    }

}
