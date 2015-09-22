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
public class FolderTest {

    Folder folder1;
    Folder folder1_copy;
    Folder folder2;

    @Before
    public void setUp() {
        folder1 = Folder.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder1").setChildCount(25).build();
        folder1_copy = Folder.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder1").setChildCount(25).build();
        folder2 = Folder.builder().setUri(Uri.parse("content://test/m/2"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder2").setChildCount(25).build();
    }

    @Test
    public void ensureFolderHashCodeWorks() {
        assertThat(folder1.hashCode()).isEqualTo(folder1_copy.hashCode());
        assertThat(folder1.hashCode()).isNotEqualTo(folder2.hashCode());
    }

    @Test
    public void ensureFolderEqualsWorks() {
        assertThat(folder1).isEqualTo(folder1_copy);
        assertThat(folder1).isNotEqualTo(folder2);
    }

    @Test
    public void ensureFolderBundleableWorks() {
        Bundle b = folder1.toBundle();
        Folder fromB = Folder.BUNDLE_CREATOR.fromBundle(b);
        assertThat(folder1).isEqualTo(fromB);
    }

    @Test(expected = NullPointerException.class)
    public void ensureFolderNullIdentityThrows() {
        Folder f = Folder.builder().setName("Folder1").build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureFolderNullNameThrows() {
        Folder f = Folder.builder().setName("Folder1").build();
    }
}
