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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.cache.ArtworkCache;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 10/21/14.
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class ArtworkRequestManagerTest {

    static final String TEST_MBID1 = "488fb0f9-1f19-4253-a2ce-a1059609484e";
    static final String TEST_MBID1_RESULT = "http://coverartarchive.org/release/488fb0f9-1f19-4253-a2ce-a1059609484e/3085051593.jpg";
    static final String TEST_MBID2 = "163c8bbc-053d-4208-ae42-4cb0dc75f050";

    @Mock AppPreferences prefs;
    @Mock ArtworkCache l1;
    @Mock BitmapDiskCache l2;
    RequestQueue queue;
    Gson gson;
    ArtworkRequestManagerImpl artworkManager;

    @Before
    public void setUp() {
        initMocks(this);
        queue = new RequestQueue(new MockCache(), new BasicNetwork(new MockHttpStack()), 1, new ImmediateResponseDelivery());
        queue.start();
        gson = new Gson();
        artworkManager = new ArtworkRequestManagerImpl(Robolectric.application, prefs, l1, l2, queue, gson);
    }

    @After
    public void tearDown() {
        queue.stop();
    }

    @Test
    public void testCoverArtRequest() {
        String url = artworkManager.createAlbumCoverArtRequestObservable(TEST_MBID1).toBlocking().first();
        assertThat(url).isEqualTo(TEST_MBID1_RESULT);
    }

    @Test
    public void testAlbumApiRequest() {
        Album album = artworkManager.createAlbumLastFmApiRequestObservable(new ArtInfo("notused", "notused", null)).toBlocking().first();
        assertThat(album.getMbid()).isEqualTo(TEST_MBID1);
    }

    @Test
    public void testArtistApiRequest() {
        Artist artist = artworkManager.createArtistLastFmApiRequestObservable(new ArtInfo("notused", null, null)).toBlocking().first();
        assertThat(artist.getMbid()).isEqualTo(TEST_MBID2);
    }

    @Test
    public void testL1CacheAlbumRequest() {
        ArtInfo artInfo = new ArtInfo("artist", "album", null);
        String cacheKey = ArtworkRequestManagerImpl.getCacheKey(artInfo, ArtworkType.THUMBNAIL);
        Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.RGB_565);
        Artwork artwork = new Artwork(bitmap, null);
        Mockito.when(l1.getArtwork(cacheKey)).thenReturn(artwork);
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        AnimatedImageView imageView = new AnimatedImageView(activity, null);
        artworkManager.newAlbumRequest(imageView, null, artInfo, ArtworkType.THUMBNAIL);
        Mockito.verify(l1).getArtwork(cacheKey);
        Mockito.verify(l2, Mockito.never()).getBitmap(cacheKey);
        assertThat(((BitmapDrawable) imageView.getDrawable()).getBitmap()).isSameAs(bitmap);
    }

}
