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

package org.opensilk.music.api.model;

/**
 * Created by drew on 10/20/14.
 */
import android.net.Uri;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 10/20/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class AlbumTest {

    Album album1;
    Album album1_copy;
    Album album2;

    @Before
    public void setUp() {
        album1 = Album.builder().setIdentity("1").setName("Album1").setArtworkUri(Uri.parse("http://example.com/Art1")).build();
        album1_copy = Album.builder().setIdentity("1").setName("Album1").setArtworkUri(Uri.parse("http://example.com/Art1")).build();
        album2 = Album.builder().setIdentity("2").setName("Album1").build();
    }

    @Test
    public void ensureAlbumHashCodeWorks() {
        assertThat(album1.hashCode()).isEqualTo(album1_copy.hashCode());
        assertThat(album1.hashCode()).isNotEqualTo(album2.hashCode());
    }

    @Test
    public void ensureAlbumEqualsWorks() {
        assertThat(album1).isEqualTo(album1_copy);
        assertThat(album1).isNotEqualTo(album2);
    }

    @Test
    public void ensureAlbumBundleableWorks() {
        Bundle b = album1.toBundle();
        Album fromB = Album.BUNDLE_CREATOR.fromBundle(b);
        assertThat(album1).isEqualTo(fromB);
    }

    @Test(expected = NullPointerException.class)
    public void ensureAlbumNullIdentityThrows() {
        Album a = Album.builder().setName("Album1").build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureAlbumNullNameThrows() {
        Album a = Album.builder().setIdentity("1").build();
    }


}
