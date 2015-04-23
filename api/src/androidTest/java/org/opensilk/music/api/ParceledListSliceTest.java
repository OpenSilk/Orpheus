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

package org.opensilk.music.api;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.test.AndroidTestCase;

import org.opensilk.music.api.model.Album;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 4/22/15.
 */
public class ParceledListSliceTest extends AndroidTestCase {

    //This doesnt work on robolectric i assume because of the strong binder
    public void testParceledListSlice() throws Exception {
        int num = 10000;
        List<Bundle> albums = new ArrayList<>(num);
        for (int ii=0; ii<num; ii++) {
            albums.add(Album.builder()
                            .setIdentity("id-"+ii)
                            .setName("name-" + ii)
                            .setArtistName("artist-"+ ii)
                            .setArtworkUri(Uri.parse("http://somehost/art/" + ii))
                            .setDate("2015-04-22")
                            .build()
                            .toBundle()
            );
        }
        ParceledListSlice<Bundle> slice = new ParceledListSlice<>(albums);
        Parcel p = Parcel.obtain();
        p.writeParcelable(slice, 0);
        p.setDataPosition(0);
        List<Bundle> albums2 = p.<ParceledListSlice<Bundle>>readParcelable(ParceledListSlice.class.getClassLoader()).getList();
        p.recycle();
        Iterator<Bundle> ii = albums.iterator();
        Iterator<Bundle> ii2 = albums2.iterator();
        while (ii.hasNext() && ii2.hasNext()) {
            assertThat(Album.BUNDLE_CREATOR.fromBundle(ii.next()))
                    .isEqualTo(Album.BUNDLE_CREATOR.fromBundle(ii2.next()));
        }

    }
}
