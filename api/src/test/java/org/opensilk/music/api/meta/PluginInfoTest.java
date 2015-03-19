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
public class PluginInfoTest {

    PluginInfo pluginInfo1;
    PluginInfo pluginInfo1_copy;
    PluginInfo pluginInfo2;

    @Before
    public void setUp() {
        pluginInfo1 = new PluginInfo("Plugin1", "Tester plugin", new ComponentName("org.opensilk.test", "TestClass"));
        pluginInfo1_copy = new PluginInfo("Plugin1", "Tester plugin", new ComponentName("org.opensilk.test", "TestClass"));
        pluginInfo2 = new PluginInfo("Plugin2", "Tester plugin", new ComponentName("org.opensilk.test", "TestClass2"));
    }

    @Test
    public void ensurePluginInfoHashCodeWorks() {
        assertThat(pluginInfo1.hashCode()).isEqualTo(pluginInfo1_copy.hashCode());
        assertThat(pluginInfo1.hashCode()).isNotEqualTo(pluginInfo2.hashCode());
    }

    @Test
    public void ensurePluginInfoEqualsWorks() {
        assertThat(pluginInfo1).isEqualTo(pluginInfo1_copy);
        assertThat(pluginInfo1).isNotEqualTo(pluginInfo2);
    }

    @Test
    public void ensurePluginInfoParcelableWorks() {
        Parcel p = Parcel.obtain();
        pluginInfo1.writeToParcel(p, 0);
        p.setDataPosition(0);
        PluginInfo fromP = PluginInfo.CREATOR.createFromParcel(p);
        assertThat(pluginInfo1).isEqualTo(fromP);
    }
}
