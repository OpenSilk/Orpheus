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

package org.opensilk.music.artwork;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.google.gson.Gson;

import org.opensilk.music.artwork.shared.GsonModule;
import org.opensilk.music.model.ArtInfo;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.opensilk.music.artwork.Util.*;

/**
 * Created by drew on 11/11/14.
 */
public class UtilTest extends AndroidTestCase {

    Gson mGson;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //This blows up on robolectric
        mGson = new GsonModule().provideGson();
    }

    public void testSerializingDeserializingArtinfo() throws Exception {
        ArtInfo ai = new ArtInfo("artist1", "album1", Uri.parse("http://somedomain/album1"));
        ArtInfo ai2 = artInfoFromBase64EncodedJson(mGson, base64EncodedJsonArtInfo(mGson, ai));
        assertThat(ai2).isEqualTo(ai);
    }

}
