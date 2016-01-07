/*
 * Copyright (c) 2016 OpenSilk Productions LLC
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

package org.opensilk.music.library.mediastore.provider;

import android.content.UriMatcher;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 1/7/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class FoldersUrisTest {

    UriMatcher mMatcher;

    @Before
    public void setup() {
        mMatcher = FoldersUris.makeMatcher("foo");
    }

    @Test
    public void testMatcher_handlesNegativeNumbers() {
        Uri uri = Uri.parse("content://foo/-917438463/folders");
        assertThat(mMatcher.match(uri)).isEqualTo(FoldersUris.M_FOLDERS);
    }

}
