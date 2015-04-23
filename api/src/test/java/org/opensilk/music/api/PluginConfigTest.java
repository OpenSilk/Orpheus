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

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 11/11/14.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config( manifest = org.robolectric.annotation.Config.NONE)
public class PluginConfigTest {

    PluginConfig config_1;

    @Before
    public void setUp() {
        config_1 = PluginConfig.builder().addAbility(PluginConfig.SEARCHABLE).addAbility(PluginConfig.SETTINGS)
                .setPickerComponent(new ComponentName("com.test", "TestClass"), null)
                .setSettingsComponent(new ComponentName("com.test", "TestClassSettings"), null)
                .build();
    }

    @Test
    public void testConfigMaterializes() {
        Bundle b = config_1.dematerialize();
        Parcel p = Parcel.obtain();
        b.writeToParcel(p, 0);
        p.setDataPosition(0);
        Bundle b2 = Bundle.CREATOR.createFromParcel(p);
        PluginConfig fromB = PluginConfig.materialize(b2);
        assertThat(config_1.apiVersion).isEqualTo(fromB.apiVersion);
        assertThat(config_1.capabilities).isEqualTo(fromB.capabilities);
        assertThat(config_1.pickerComponent).isEqualTo(fromB.pickerComponent);
        assertThat(config_1.<ComponentName>getMeta(PluginConfig.META_SETTINGS_COMPONENT))
                .isEqualTo(fromB.<ComponentName>getMeta(PluginConfig.META_SETTINGS_COMPONENT));
    }

}
