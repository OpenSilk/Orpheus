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

package org.opensilk.music.index;

import com.squareup.okhttp.OkHttpClient;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.database.IndexDatabaseImpl;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import de.umass.lastfm.MusicEntryConverter;
import de.umass.lastfm.ResponseBuilder;
import de.umass.lastfm.Result;
import retrofit.Call;
import retrofit.Callback;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by drew on 9/16/15.
 */
@Module(
        includes = {
                IndexProviderAuthorityModule.class,
        }
)
public class IndexTestModule {
    @Provides @Singleton
    IndexDatabase provideIndexDatabase(IndexDatabaseImpl impl) {
        return impl;
    }

    @Provides @Singleton
    OkHttpClient provideOkClient() {
        return new OkHttpClient();
    }

    @Provides @Singleton
    LastFM provideLastFM() {
        return new LastFM() {
            @Override
            public Call<Artist> getArtist(@Query("artist") final String artist) {
                return new Call<Artist>() {
                    @Override
                    public retrofit.Response<Artist> execute() throws IOException {
                        try {
                            Result result = MusicEntryConverter.createResultFromInputStream(
                                    getClass().getClassLoader().getResourceAsStream(makeArtist(artist))
                            );
                            return retrofit.Response.success(
                                    ResponseBuilder.buildItem(result, Artist.class)
                            );
                        } catch (SAXException|NullPointerException e) {
                            throw  new IOException(e);
                        }
                    }

                    @Override
                    public void enqueue(Callback<Artist> callback) {
                        throw  new UnsupportedOperationException();
                    }

                    @Override
                    public void cancel() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Call<Artist> clone() {
                        return this;
                    }
                };
            }

            @Override
            public Call<Artist> getArtistbyMbid(@Query("mbid") String mbid) {
                return null;
            }

            @Override
            public Call<Album> getAlbumbyMbid(@Query("mbid") String mbid) {
                return null;
            }

            @Override
            public Call<Album> getAlbum(@Query("artist") final String artist, @Query("album") final String album) {
                return new Call<Album>() {
                    @Override
                    public retrofit.Response<Album> execute() throws IOException {
                        try {
                            Result result = MusicEntryConverter.createResultFromInputStream(
                                    getClass().getClassLoader().getResourceAsStream(makeAlbum(artist, album))
                            );
                            return retrofit.Response.success(
                                    ResponseBuilder.buildItem(result, Album.class)
                            );
                        } catch (SAXException|NullPointerException e) {
                            throw new IOException(e);
                        }
                    }

                    @Override
                    public void enqueue(Callback<Album> callback) {
                        throw  new UnsupportedOperationException();
                    }

                    @Override
                    public void cancel() {
                        throw  new UnsupportedOperationException();
                    }

                    @Override
                    public Call<Album> clone() {
                        return this;
                    }
                };


            }

            @Override
            public Observable<Artist> getArtistObservable(@Query("artist") String artist) {
                return null;
            }

            @Override
            public Observable<Album> getAlbumObservable(@Query("artist") String artist, @Query("album") String album) {
                return null;
            }
        };
    }

    static String makeArtist(String artist) {
        return String.format("lfm_artist_%s.xml", artist.toLowerCase());
    }

    static String makeAlbum(String artist, String album) {
        return String.format("lfm_album_%s_%s.xml", artist.toLowerCase(), album.toLowerCase());
    }

    String readResource(String name) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        String res = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        return res;
    }

}
