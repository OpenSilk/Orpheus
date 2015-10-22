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

package org.opensilk.music.artwork.fetcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.BuildConfig;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.coverartarchive.CoverArtArchive;
import org.opensilk.music.artwork.coverartarchive.CoverArtArchiveModule;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.lastfm.LastFMModule;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.okhttp.OkHttpModule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;

import javax.inject.Named;

import de.umass.lastfm.LastFM;
import okio.Buffer;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by drew on 10/5/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP
)
public class ArtworkFetcherManagerTest {

    @Rule public final MockWebServer mServer= new MockWebServer();

    @Mock Context mContext;
    @Mock ArtworkPreferences mPreferences;
    @Mock BitmapDiskCache mL2Cache;
    @Mock ConnectivityManager mConnectivityManager;
    @Spy TestListener mListener;

    TestFetcher mFetcher;

    private static class TestListener extends CompletionListener {
        //set true to debug unexpected errors
        boolean printStack = false;
        @Override public void onError(Throwable e) {
                if (printStack)e.printStackTrace(System.err);
        }
        @Override public void onCompleted() { }
    }

    private static class TestFetcher extends ArtworkFetcherManager {
        public TestFetcher(@ForApplication Context mContext, ArtworkPreferences mPreferences, BitmapDiskCache mL2Cache, ConnectivityManager mConnectivityManager, @Named("ObserveOnScheduler") Scheduler mObserveOn, @Named("SubscribeOnScheduler") Scheduler mSubscribeOn, LastFM mLastFM, CoverArtArchive mCoverArtArchive, OkHttpClient mOkHttpClient, MockWebServer mServer) {
            super(mContext, mPreferences, mL2Cache, mConnectivityManager, mObserveOn, mSubscribeOn, mLastFM, mCoverArtArchive, mOkHttpClient);
            this.mServer = mServer;
        }
        final MockWebServer mServer;
        String originalUri = null;
        @Override
        protected String mangleImageUrl(String url) {
            originalUri = url;
            return mServer.url("/image/").toString();
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        LastFMModule lfm = new LastFMModule();
        OkHttpClient okHttpClient = new OkHttpModule().provideOkClient(RuntimeEnvironment.application);
        OkHttpClient client = lfm.provideOkHttpClient(okHttpClient, "foo", mServer.url("/lfm/").toString());
        LastFM lastFM = lfm.provideLastFM(mServer.url("/lfm/").toString(), client);
        CoverArtArchiveModule caa= new CoverArtArchiveModule();
        CoverArtArchive coverArtArchive = caa.provideCoverArtArchive(client, mServer.url("/caa/").toString());
        mFetcher = new TestFetcher(
                mContext,
                mPreferences,
                mL2Cache,
                mConnectivityManager,
                Schedulers.immediate(),
                Schedulers.immediate(),
                lastFM,
                coverArtArchive,
                client,
                mServer
        );
    }

    @Test
    public void testAlbumFetching1NoPreferDownloadUsesProvidedUri() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = false;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse("content://test/alvvays.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //no to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(false);
        final ContentResolver contentResolver = mock(ContentResolver.class);
        when(contentResolver.openInputStream(Uri.parse("content://test/alvvays.jpg"))).thenReturn(
                getClass().getClassLoader().getResourceAsStream("alvvays_alvvays_thumb.jpg"));
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        //we have to use a real instance here, so we spy on it
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verify(mContext).getContentResolver();
    }

    @Test
    public void testAlbumFetching1NoPreferDownloadUsesProvidedUri2() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = false;
        isLocalArt = false;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse(mServer.url("/alvvays.jpg").toString()));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //no to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(false);
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
//        mListener.printStack = true;
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching1YesPreferDownload() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse("content://test/alvvays.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_album_alvvays_alvvays.xml"))));
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("caa_release_0ea1ee01-54b6-439f-bf01-250375741813.json"))));
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isEqualTo("http://coverartarchive.org/release/0ea1ee01-54b6-439f-bf01-250375741813/7307203963.png");
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching1YesPreferDownloadLastFMErrorFallsback() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse("content://test/alvvays.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setResponseCode(400).setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_error_album_not_found.xml"))));
        final ContentResolver contentResolver = mock(ContentResolver.class);
        when(contentResolver.openInputStream(Uri.parse("content://test/alvvays.jpg"))).thenReturn(
                getClass().getClassLoader().getResourceAsStream("alvvays_alvvays_thumb.jpg"));
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verify(mContext).getContentResolver();
    }

    @Test
    public void testAlbumFetching1YesPreferDownloadCaaErrorFallsBackToLastFM() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse("content://test/alvvays.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_album_alvvays_alvvays.xml"))));
        mServer.enqueue(new MockResponse().setResponseCode(404));
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isEqualTo("http://img2-ak.lst.fm/i/u/63ea8d50b43146e7c64414891c20d378.png");
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching1NoWantArtDownloadsRegardlessWhenRemoteUri() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = false;
        preferDownload = true;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse(mServer.url("/alvvays.jpg").toString()));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //no to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(false);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching1NoPreferDownloadGetsLocalArtWhenOffline() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = false
        wantAlbumArt = true;
        preferDownload = false;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", Uri.parse("content://test/art.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //not connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //no to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(false);
        final ContentResolver contentResolver = mock(ContentResolver.class);
        when(contentResolver.openInputStream(Uri.parse("content://test/art.jpg"))).thenReturn(
                getClass().getClassLoader().getResourceAsStream("alvvays_alvvays_thumb.jpg"));
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verify(mContext).getContentResolver();
    }

    @Test
    public void testAlbumFetching2NoUriLastFMErrorFails() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = false;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = false;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", null);
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setResponseCode(400).setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_error_album_not_found.xml"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener, never()).onNext(Matchers.<Bitmap>any());
        verify(mListener, never()).onCompleted();
        verify(mListener).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching3UriOnlyAlwaysFetchesWhenOnline() throws IOException {
        /*
        hasAlbumArtist = false;
        hasUri = true;
        isOnline = true
        wantAlbumArt = false;
        preferDownload = false;
        isLocalArt = false;
         */
        ArtInfo artInfo = ArtInfo.forAlbum(null, null, Uri.parse(mServer.url("/alvvays.jpg").toString()));
        System.out.println(artInfo);
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(false);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(false);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testAlbumFetching4LastFMNoMbidFallsback() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = true;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = true;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("various artists", "guther d", Uri.parse("content://test/art.jpg"));
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_album_gunther_d_botsautomix.xml"))));
        final ContentResolver contentResolver = mock(ContentResolver.class);
        when(contentResolver.openInputStream(Uri.parse("content://test/art.jpg"))).thenReturn(
                getClass().getClassLoader().getResourceAsStream("alvvays_alvvays_thumb.jpg"));
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verify(mContext).getContentResolver();
    }

    @Test
    public void testAlbumFetching2LastFMErrorNoUriFails() throws IOException {
        /*
        hasAlbumArtist = true;
        hasUri = false;
        isOnline = true
        wantAlbumArt = true;
        preferDownload = true;
        isLocalArt = false;
         */
        ArtInfo artInfo = ArtInfo.forAlbum("alvvays", "alvvays", null);
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        //yes to want
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTWORK, true)).thenReturn(true);
        //yes to download
        when(mPreferences.getBoolean(ArtworkPreferences.PREFER_DOWNLOAD_ARTWORK, false)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setResponseCode(400).setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_error_album_not_found.xml"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener, never()).onNext(Matchers.<Bitmap>any());
        verify(mListener, never()).onCompleted();
        verify(mListener).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testArtistFetching1() throws IOException {
        /*
        isOnline = true;
        wantArtistImages = true;
         */
        ArtInfo artInfo = ArtInfo.forArtist("alvvays", null);
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_artist_alvvays.xml"))));
        mServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(getClass().getClassLoader()
                .getResourceAsStream("alvvays_alvvays_thumb.jpg"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener).onNext(Matchers.<Bitmap>any());
        verify(mListener).onCompleted();
        verify(mListener, never()).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isEqualTo("http://img2-ak.lst.fm/i/u/ba772c4e45294e41cebccd40acb9e4aa.png");
        verifyZeroInteractions(mContext);
    }

    @Test
    public void testArtistFetching2NoImagesFails() throws IOException {
        /*
        isOnline = true;
        wantArtistImages = true;
         */
        ArtInfo artInfo = ArtInfo.forArtist("pretty lights feat. talib kweli", null);
        //no to wifi
        when(mPreferences.getBoolean(ArtworkPreferences.ONLY_ON_WIFI, true)).thenReturn(false);
        //yes were connected
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        //no to cache
        when(mL2Cache.containsKey(artInfo.cacheKey())).thenReturn(false);
        when(mPreferences.getBoolean(ArtworkPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true)).thenReturn(true);
        //prep the server
        mServer.enqueue(new MockResponse().setBody(IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("lfm_artist_pretty_lights_feat_talib_kweli.xml"))));
        mFetcher.fetch(artInfo, mListener);
        verify(mListener, never()).onNext(Matchers.<Bitmap>any());
        verify(mListener, never()).onCompleted();
        verify(mListener).onError(Matchers.<Throwable>any());
        assertThat(mFetcher.originalUri).isNullOrEmpty();
        verifyZeroInteractions(mContext);
    }

}
