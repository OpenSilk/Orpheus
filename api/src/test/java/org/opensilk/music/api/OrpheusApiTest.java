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

package org.opensilk.music.api;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.api.model.Folder;
import org.opensilk.music.api.model.spi.Bundleable;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 10/20/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class OrpheusApiTest {

    @Test
    public void testMaterilizeBundleWorks() throws Exception {
        Folder f = new Folder.Builder().setIdentity("1").setName("Folder1").build();
        Bundle b = f.toBundle();

        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b1 = p.readBundle();

        Bundleable b2 = OrpheusApi.materializeBundle(b1);
        Assert.assertTrue((b2 instanceof Folder));
        assertThat((Folder) b2).isEqualTo(f);

        Folder f2 = OrpheusApi.materializeBundle(Folder.class, b);
        assertThat(f).isEqualTo(f2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMalformedBundleThrows() throws Exception {
        Bundle b =  new Folder.Builder().setIdentity("1").setName("Folder1").build().toBundle();
        b.putString("clz", null);
        Folder f = OrpheusApi.materializeBundle(Folder.class, b);
    }
}
