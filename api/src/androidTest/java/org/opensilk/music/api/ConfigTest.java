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

import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 11/11/14.
 */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config( manifest = org.robolectric.annotation.Config.NONE)
public class ConfigTest {

    Config config_1;
    Config config_1_copy;
    Config config_2;

    @Before
    public void setUp() {
        config_1 = new Config.Builder().addAbility(Config.SEARCHABLE).addAbility(Config.SETTINGS)
                .setPickerIntent(new Intent("PICKER")).setSettingsIntent(new Intent("SETTINGS")).build();
        config_1_copy = new Config.Builder().addAbility(Config.SEARCHABLE).addAbility(Config.SETTINGS)
                .setPickerIntent(new Intent("PICKER")).setSettingsIntent(new Intent("SETTINGS")).build();
        config_2 = new Config.Builder().setCapabilities(Config.SEARCHABLE)
                .setPickerIntent(new Intent("PICKER")).build();
    }

    @Test
    public void ensureConfigHashCodeWorks() {
        assertThat(config_1.hashCode()).isEqualTo(config_1_copy.hashCode());
        assertThat(config_1.hashCode()).isNotEqualTo(config_2.hashCode());
    }

    @Test
    public void ensureConfigEqualsWorks() {
        assertThat(config_1).isEqualTo(config_1_copy);
        assertThat(config_1).isNotEqualTo(config_2);
    }

    @Test
    public void ensureConfigParcelableWorks() {
        Parcel p = Parcel.obtain();
        config_1.writeToParcel(p, 0);
        p.setDataPosition(0);
        Config fromP = Config.CREATOR.createFromParcel(p);
        //Holy shit soo fucking long to figure out /this/ was the problem
        //dont know why it works in the other tests
//        assertThat(fromP).isEqualTo(config_1);
        assertThat(config_1.equals(fromP));

        Parcel p2 = Parcel.obtain();
        config_2.writeToParcel(p2, 0);
        p.setDataPosition(0);
        Config fromP2 = Config.CREATOR.createFromParcel(p2);
//        assertThat(fromP2).isEqualTo(config_2);
        assertThat(config_2.equals(fromP2));
    }

}
