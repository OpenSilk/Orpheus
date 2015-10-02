/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 10/2/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class MetadataTest {

    @Test
    public void testAllKeysAreUnique() throws Exception {
        Field[] fields = Metadata.class.getDeclaredFields();
        Set<String> keySet = new HashSet<>();
        int ii=0;
        for (Field f : fields) {
            if (StringUtils.startsWith(f.getName(), "KEY_")) {
                ii++;
                keySet.add((String)f.get(null));
            }
        }
        assertThat(keySet.size()).isEqualTo(ii);
    }

}
