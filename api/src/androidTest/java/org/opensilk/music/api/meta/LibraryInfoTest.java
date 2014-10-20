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

package org.opensilk.music.api.meta;

import android.content.ComponentName;
import android.os.Parcel;

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
public class LibraryInfoTest {

    LibraryInfo libraryInfo1;
    LibraryInfo libraryInfo1_copy;
    LibraryInfo libraryInfo2;

    @Before
    public void setUp() {
        libraryInfo1 = new LibraryInfo("1", new ComponentName("org.opensilk.test", "TestClass"), "1");
        libraryInfo1_copy = new LibraryInfo("1", new ComponentName("org.opensilk.test", "TestClass"), "1");
        libraryInfo2 = new LibraryInfo("11", new ComponentName("org.opensilk.test1", "TestClass"), "11");
    }

    @Test
    public void ensureLibraryInfoHashCodeWorks() {
        assertThat(libraryInfo1.hashCode()).isEqualTo(libraryInfo1_copy.hashCode());
        assertThat(libraryInfo1.hashCode()).isNotEqualTo(libraryInfo2.hashCode());
    }

    @Test
    public void ensureLibraryInfoEqualsWorks() {
        assertThat(libraryInfo1).isEqualTo(libraryInfo1_copy);
        assertThat(libraryInfo1).isNotEqualTo(libraryInfo2);
    }

    @Test
    public void ensureLibraryInfoParcelableWorks() {
        Parcel parcel = Parcel.obtain();
        libraryInfo1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        LibraryInfo fromParcel = LibraryInfo.CREATOR.createFromParcel(parcel);
        assertThat(libraryInfo1).isEqualTo(fromParcel);
    }

}
