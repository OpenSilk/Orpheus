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

package org.opensilk.music.index.scanner;

import android.os.Build;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexTestApplication;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by drew on 9/24/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class MetaExtractorImplTest {

    @Test
    public void testParseTrackNum() {
        int track = MetaExtractorImpl.parseTrackNum("3/10");
        Assertions.assertThat(track).isEqualTo(3);
        track = MetaExtractorImpl.parseTrackNum("4");
        Assertions.assertThat(track).isEqualTo(4);
    }

    @Test
    public void testParseDiscNum() {
        int disc = MetaExtractorImpl.parseDiskNum("1/1");
        Assertions.assertThat(disc).isEqualTo(1);
        disc = MetaExtractorImpl.parseDiskNum("2");
        Assertions.assertThat(disc).isEqualTo(2);
    }

}
