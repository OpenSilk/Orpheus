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

package org.opensilk.music.playback.control;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.opensilk.music.playback.control.PlaybackControllerImpl.*;

/**
 * Created by drew on 10/23/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PlaybackControllerTest {

    @Test
    public void testClampList() {
        List<Uri> list = new ArrayList<>(1000);
        for (int ii=0; ii<1000; ii++) {
            list.add(Uri.parse("content://foo/"+ii));
        }
        assertThat(clampList(list, 0))
                .isEqualTo(list.subList(0, maxListsize));
        assertThat(clampList(list, 10))
                .isEqualTo(list.subList(0, maxListsize));
        assertThat(clampList(list, 500))
                .isEqualTo(list.subList(500 - maxListsize / 2, 500 + maxListsize / 2));
        assertThat(clampList(list, 990))
                .isEqualTo(list.subList(list.size()-maxListsize, list.size()));
    }

}
