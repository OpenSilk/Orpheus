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

package org.opensilk.music.ui3;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.BuildConfig;
import org.opensilk.music.TestApp;
import org.opensilk.music.model.Artist;
import org.opensilk.music.ui3.profile.artist.ArtistDetailsScreen;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by drew on 11/16/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        application = TestApp.class,
        sdk = 21
)
public class ProfileActivityTest {
    @Test
    public void testActivityStarts() {
        ProfileActivity activity = Robolectric.buildActivity(ProfileActivity.class)
                .withIntent(ProfileActivity.makeIntent(RuntimeEnvironment.application,
                        new ArtistDetailsScreen(Artist.builder()
                                .setName("artist1")
                                .setUri(Uri.parse("content://foo/artist1"))
                                .setParentUri(Uri.parse("content://foo/artists"))
                                .build())))
                .create().start().resume().visible().get();
    }
}
