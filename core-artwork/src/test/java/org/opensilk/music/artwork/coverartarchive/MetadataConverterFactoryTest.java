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

package org.opensilk.music.artwork.coverartarchive;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import retrofit.Response;
import retrofit.Retrofit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 10/4/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MetadataConverterFactoryTest {

    @Rule public final MockWebServer server = new MockWebServer();

    CoverArtArchive service;

    @Before
    public void setup() {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(new MetadataConverterFactory())
                .baseUrl(server.url("/"))
                .build();
        service = retrofit.create(CoverArtArchive.class);
    }

    @Test
    public void testMetadataConverter() throws Exception {
        server.enqueue(new MockResponse().setBody(
                IOUtils.toString(getClass().getClassLoader()
                        .getResourceAsStream("caa_release_0ea1ee01-54b6-439f-bf01-250375741813.json"))
        ));
        Response<Metadata> response = service.getRelease("0ea1ee01-54b6-439f-bf01-250375741813").execute();
        assertThat(response.isSuccess()).isTrue();
        Metadata body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.images).isNotEmpty();
        Metadata.Image image = body.images.get(0);
        assertThat(image.image).isNotNull();
        assertThat(image.front).isTrue();
        assertThat(image.approved).isTrue();
    }

}
