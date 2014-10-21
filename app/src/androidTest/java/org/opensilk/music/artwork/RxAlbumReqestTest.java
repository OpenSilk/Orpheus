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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;


import de.umass.lastfm.Album;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by drew on 10/21/14.
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class RxAlbumReqestTest {

    RequestQueue requestQueue;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        requestQueue = Volley.newRequestQueue(activity);

    }

    @After
    public void tearDown() {
        requestQueue.stop();
    }

    @Test
    public void testCoverArtRequest() {
//        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
//
//        String result = Observable.create(new Observable.OnSubscribe<String>() {
//            @Override
//            public void call(Subscriber<? super String> subscriber) {
//                String mbid = "76df3287-6cda-33eb-8e9a-044b5e15ffdd";
//                RxAlbumRequest.CoverArtRequestListener listener = new RxAlbumRequest.CoverArtRequestListener(subscriber);
//                RxAlbumRequest.CoverArtRequest coverArtRequest = new RxAlbumRequest.CoverArtRequest(mbid, listener);
//                RequestFuture<RxAlbumRequest.CoverArtRequest> f = RequestFuture.newFuture();
//                requestQueue.add(coverArtRequest);
//            }
//        }).toBlocking().first();
//        Assertions.assertThat(result).isNotEqualTo("other");
    }
}
