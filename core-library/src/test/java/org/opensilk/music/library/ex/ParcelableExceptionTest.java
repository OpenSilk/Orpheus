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

package org.opensilk.music.library.ex;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.library.internal.LibraryException;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 10/20/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class ParcelableExceptionTest {

    @Test
    public void testParcelableExceptionWorks() {
        LibraryException ex =
                new LibraryException(LibraryException.Kind.NETWORK, new IOException("No network"));
        Parcel p = Parcel.obtain();
        ex.writeToParcel(p, 0);
        p.setDataPosition(0);
        LibraryException fromP = LibraryException.CREATOR.createFromParcel(p);
        assertThat(ex.getCode() == fromP.getCode());
        assertThat(ex.getCause().toString().equals(fromP.getCause().toString()));
    }

}
