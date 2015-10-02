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

package org.opensilk.bundleable;

import android.os.Bundle;
import android.os.Parcel;

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
public class BundleableUtilTest {

    @Test
    public void testMaterializeBundleWorks() throws Exception {
        TestBundleable t = new TestBundleable("fred", 26);
        Bundle b = t.toBundle();

        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b1 = p.readBundle();

        Bundleable b2 = BundleableUtil.materializeBundle(b1);
        assertThat((b2 instanceof TestBundleable)).isTrue();
        assertThat((TestBundleable) b2).isEqualTo(t);
    }

    @Test
    public void testMaterializeBundleWorks2() throws Exception {
        TestBundleable t = new TestBundleable("sally", 23);
        Bundle b = t.toBundle();

        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b1 = p.readBundle();

        TestBundleable f2 = BundleableUtil.materializeBundle(TestBundleable.class, b1);
        assertThat(t).isEqualTo(f2);
    }

    @Test(expected = BadBundleableException.class)
    public void testWrongClassThrows() throws Exception {
        TestBundleable t = new TestBundleable("alice", 32);
        Bundle b = t.toBundle();
        BundleableUtil.materializeBundle("org.opensilk.Foo", b);
    }
}
