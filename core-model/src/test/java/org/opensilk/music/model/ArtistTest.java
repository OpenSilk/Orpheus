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

package org.opensilk.music.model;

import android.net.Uri;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Created by drew on 10/20/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class ArtistTest {

    Artist artist1;
    Artist artist1_copy;
    Artist artist2;

    @Before
    public void setUp() {
        artist1 = Artist.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Artist1").setTrackCount(25).build();
        artist1_copy = Artist.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Artist1").setTrackCount(25).build();
        artist2 = Artist.builder().setUri(Uri.parse("content://test/m/2"))
                .setParentUri(Uri.parse("content://test/m")).setName("Artist1").setTrackCount(22).build();
    }

    @Test
    public void ensureArtistHashCodeWorks() {
        assertThat(artist1.hashCode()).isEqualTo(artist1_copy.hashCode());
        assertThat(artist1.hashCode()).isNotEqualTo(artist2.hashCode());
    }

    @Test
    public void ensureArtistEqualsWorks() {
        assertThat(artist1).isEqualTo(artist1_copy);
        assertThat(artist1).isNotEqualTo(artist2);
    }

    @Test
    public void ensureArtistBundleableWorks() {
        Bundle b = artist1.toBundle();
        Artist fromB = Artist.BUNDLE_CREATOR.fromBundle(b);
        assertThat(artist1).isEqualTo(fromB);
    }

    @Test(expected = NullPointerException.class)
    public void ensureArtistNullIdentityThrows() {
        Artist a = Artist.builder().setName("Artist1").build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureArtistNullNameThrows() {
        Artist a = Artist.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).build();
    }

}
