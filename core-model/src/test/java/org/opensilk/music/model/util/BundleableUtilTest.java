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

package org.opensilk.music.model.util;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.ex.BadBundleableException;
import org.opensilk.music.model.spi.Bundleable;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 10/20/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class BundleableUtilTest {

    @Test
    public void testMaterializeBundleWorks() throws Exception {
        Folder f = Folder.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder1").build();
        Bundle b = f.toBundle();

        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b1 = p.readBundle();

        Bundleable b2 = BundleableUtil.materializeBundle(b1);
        Assert.assertTrue((b2 instanceof Folder));
        assertThat((Folder) b2).isEqualTo(f);
    }

    @Test
    public void testMaterializeBundleWorks2() throws Exception {
        Folder f = Folder.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder1").build();
        Bundle b = f.toBundle();

        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b1 = p.readBundle();

        Folder f2 = BundleableUtil.materializeBundle(Folder.class, b);
        assertThat(f).isEqualTo(f2);
    }

    @Test(expected = BadBundleableException.class)
    public void testWrongClassThrows() throws Exception {
        Bundle b =  Folder.builder().setUri(Uri.parse("content://test/m/1"))
                .setParentUri(Uri.parse("content://test/m")).setName("Folder1").build().toBundle();
        BundleableUtil.materializeBundle(Artist.class.getName(), b);
    }
}
