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

package org.opensilk.music.core.model;

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
public class TrackTest {

    Track song1;
    Track song1_copy;
    Track song2;

    @Before
    public void setUp() {
        song1 = Track.builder().setIdentity("1").setName("Song1").setDataUri(Uri.parse("http://example.com/song1")).build();
        song1_copy = Track.builder().setIdentity("1").setName("Song1").setDataUri(Uri.parse("http://example.com/song1")).build();
        song2 = Track.builder().setIdentity("1").setName("Song1").setDataUri(Uri.parse("http://example.com/song1")).setMimeType("audio/mp3").build();
    }

    @Test
    public void ensureSongHashCodeWorks() {
        assertThat(song1.hashCode()).isEqualTo(song1_copy.hashCode());
        assertThat(song1.hashCode()).isNotEqualTo(song2.hashCode());
    }

    @Test
    public void ensureSongEqualsWorks() {
        assertThat(song1).isEqualTo(song1_copy);
        assertThat(song1).isNotEqualTo(song2);
    }

    @Test
    public void ensureSongBundleableWorks() {
        Bundle b = song1.toBundle();
        Track fromB = Track.BUNDLE_CREATOR.fromBundle(b);
        assertThat(song1).isEqualTo(fromB);
    }

    @Test(expected = NullPointerException.class)
    public void ensureSongNullIdentityThrows() {
        Track.builder().setName("Song1").setDataUri(Uri.parse("http://example.com/song1")).build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureSongNullNameThrows() {
        Track.builder().setIdentity("1").setDataUri(Uri.parse("http://example.com/song1")).build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureSongNullDataUriThrows() {
        Track.builder().setIdentity("1").setName("Song1").build();
    }

    @Test
    public void ensureSongDefaultMimeTypeWasSet() {
        assertThat(song1.mimeType).isEqualTo(Track.DEFAULT_MIME_TYPE);
    }

    @Test
    public void ensureSongDefaultMimeTypeWasOverridden() {
        assertThat(song2.mimeType).isNotEqualTo(Track.DEFAULT_MIME_TYPE);
    }
}
