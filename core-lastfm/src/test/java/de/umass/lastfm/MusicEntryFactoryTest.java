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

package de.umass.lastfm;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import retrofit.Call;
import retrofit.Response;
import retrofit.Retrofit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by drew on 10/4/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MusicEntryFactoryTest {

    @Rule public final MockWebServer server = new MockWebServer();
    LastFM mLastFm;

    @Before
    public void setup() {
        Retrofit r = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(new MusicEntryFactory())
                .build();
        mLastFm = r.create(LastFM.class);
    }

    @Test
    public void testAlbumResponse() throws Exception {
        String body = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("lfm_album_alvvays_alvvays.xml"));
        server.enqueue(new MockResponse().setBody(body));

        Call<Album> call = mLastFm.getAlbum("alvvays", "alvvays");
        Response<Album> response = call.execute();
        Album album = response.body();
        assertThat(album).isNotNull();
        assertThat(album.getName()).isEqualToIgnoringCase("alvvays");
        assertThat(album.getArtist()).isEqualToIgnoringCase("alvvays");
    }

    @Test
    public void testArtistResponse() throws Exception {
        String body = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("lfm_artist_alvvays.xml"));
        server.enqueue(new MockResponse().setBody(body));

        Call<Artist> call = mLastFm.getArtist("alvvays");
        Response<Artist> response = call.execute();
        Artist artist = response.body();
        assertThat(artist).isNotNull();
        assertThat(artist.getName()).isEqualToIgnoringCase("alvvays");
    }

}
