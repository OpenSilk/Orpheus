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

package org.opensilk.music.artwork;

import android.app.Activity;
import android.content.Context;
import android.util.JsonReader;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opensilk.music.TestMusicApp;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;


import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.umass.lastfm.Album;
import mortar.Mortar;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by drew on 10/21/14.
 */
@Config(emulateSdk = 18, reportSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ArtworkRequestManagerTest {

    static final String TEST_MBID1 = "5f482bbe-d747-4b10-b0a9-2b0873f24928";
    static final String TEST_MBID1_RESPONSE = "{\"images\":[{\"types\":[\"Front\"],\"front\":true,\"back\":false,\"edit\"" +
            ":24993236,\"image\":\"http://coverartarchive.org/release/5f482bbe-d747-4b10-b0a9-2b0873f24928/5806163660.jpg\"," +
            "\"comment\":\"\",\"approved\":true,\"id\":\"5806163660\",\"thumbnails\":{\"large\":" +
            "\"http://coverartarchive.org/release/5f482bbe-d747-4b10-b0a9-2b0873f24928/5806163660-500.jpg\"," +
            "\"small\":\"http://coverartarchive.org/release/5f482bbe-d747-4b10-b0a9-2b0873f24928/5806163660-250.jpg\"}}]" +
            ",\"release\":\"http://musicbrainz.org/release/5f482bbe-d747-4b10-b0a9-2b0873f24928\"}";
    static final String TEST_MBID1_RESULT = "http://coverartarchive.org/release/5f482bbe-d747-4b10-b0a9-2b0873f24928/5806163660.jpg";

    @Inject
    ArtworkRequestManagerImpl artworkManager;

    @Before
    public void setUp() {
        TestMusicApp.injectTest(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCoverArtRequest() {
        String url = artworkManager.createAlbumCoverArtRequestObservable(TEST_MBID1).toBlocking().first();
        Assert.assertEquals(url,TEST_MBID1_RESULT);
    }


    @Test
    public void testAlbumRequest() {

    }

}
