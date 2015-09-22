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
import android.os.Parcel;

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
public class ArtInfoTest {

    ArtInfo artInfo1;
    ArtInfo artInfo1_copy;
    ArtInfo artInfo2;

    @Before
    public void setUp() {
        artInfo1 = new ArtInfo("Artist1", "Album1", Uri.parse("http://example.com/get/Artist1"));
        artInfo1_copy = new ArtInfo("Artist1", "Album1", Uri.parse("http://example.com/get/Artist1"));
        artInfo2 = new ArtInfo("Artist2", "Album2", Uri.parse("http://example.org/get/Artist2"));
    }

    @Test
    public void ensureArtInfoHashCodeWorks() {
        assertThat(artInfo1.hashCode()).isEqualTo(artInfo1_copy.hashCode());
        assertThat(artInfo1.hashCode()).isNotEqualTo(artInfo2.hashCode());
    }

    @Test
    public void ensureArtInfoEqualsWorks() {
        assertThat(artInfo1).isEqualTo(artInfo1_copy);
        assertThat(artInfo1).isNotEqualTo(artInfo2);
    }

    @Test
    public void ensureArtInfoParcelableWorks() {
        Parcel p = Parcel.obtain();
        artInfo1.writeToParcel(p, 0);
        p.setDataPosition(0);
        ArtInfo fromP = ArtInfo.CREATOR.createFromParcel(p);
        assertThat(artInfo1).isEqualTo(fromP);
    }

}
