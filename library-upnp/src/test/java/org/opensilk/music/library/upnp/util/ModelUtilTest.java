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

package org.opensilk.music.library.upnp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opensilk.music.library.upnp.util.ModelUtil.*;

/**
 * Created by drew on 11/25/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class ModelUtilTest {

    @Test
    public void testDurationParcer() {
        String duration = "0:00:25.678";
        assertThat(parseDuration(duration)).isEqualTo(25);
        duration = "0:05:53.645";
        assertThat(parseDuration(duration)).isEqualTo(353);
        duration = "0:05:53.1/2";
        assertThat(parseDuration(duration)).isEqualTo(353);
        duration = "0:05:53";
        assertThat(parseDuration(duration)).isEqualTo(353);
        duration = ":05:53.645";
        assertThat(parseDuration(duration)).isEqualTo(353);
        duration = "2:05:53.645";
        assertThat(parseDuration(duration)).isEqualTo(7553);
    }
}
