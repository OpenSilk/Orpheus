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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.view.LayoutInflater;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.MortarTestActivity;
import org.opensilk.music.R;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import mortar.MortarScope;

import static org.fest.assertions.api.ANDROID.assertThat;

/**
 * Created by drew on 10/19/14.
 */
@Config(
        emulateSdk = 18,
        reportSdk = 18
)
@RunWith(RobolectricTestRunner.class)
public class GalleryViewTest {

    GalleryView galleryView;

    @Before
    public void setup() {
        MortarTestActivity activity = Robolectric.buildActivity(MortarTestActivity.class).create().get();
        MortarScope galleryScope = activity.mActivityScope.requireChild(new GalleryScreen());
        Context galleryContext = galleryScope.createContext(activity);
        galleryView = (GalleryView) LayoutInflater.from(galleryContext).inflate(R.layout.gallery, null);
    }

    @Test
    public void galleryViewWasInflated() {
        assertThat(galleryView).isNotNull();
    }

}
