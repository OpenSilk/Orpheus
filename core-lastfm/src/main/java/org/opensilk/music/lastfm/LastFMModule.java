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

package org.opensilk.music.lastfm;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.opensilk.music.okhttp.OkHttpModule;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.umass.lastfm.LastFM;
import de.umass.lastfm.MusicEntryFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import timber.log.Timber;

/**
 * Created by drew on 9/16/15.
 */
@Module(
        includes = OkHttpModule.class
)
public class LastFMModule {


    @Provides @Singleton
    public LastFM provideLastFM(
            final @Named("LFMEndpoint") String endpoint,
            @Named("LastFM") OkHttpClient client
    ) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addConverterFactory(new MusicEntryFactory())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();
        return retrofit.create(LastFM.class);
    }

    @Provides @Named("LFMEndpoint")
    public String provideLfmendpoint() {
        return LastFM.API_ROOT;
    }

    @Provides @Named("LFMApiKey")
    public String provideLfmapiKEy() {
        return BuildConfig.LASTFM_KEY;
    }

    @Provides @Singleton @Named("LastFM")
    public OkHttpClient provideOkHttpClient(
            OkHttpClient okHttpClient,
            final @Named("LFMApiKey") String apiKey,
            final @Named("LFMEndpoint") String endpoint
    ) {
        OkHttpClient client = okHttpClient.clone();
        client.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                //recover the original request
                final Request ogReq = chain.request();
                final Request newReq;
                if (ogReq.urlString().startsWith(endpoint)) {
                    //inject our global query parameter
                    HttpUrl newUrl = ogReq.httpUrl().newBuilder()
                            .addQueryParameter("api_key", apiKey)
                            .addQueryParameter("lang", Locale.getDefault().getLanguage())
//                            .addQueryParameter("autocorrect", "1")
                            .build();
                    //update request with new url
                    newReq = ogReq.newBuilder()
                            .url(newUrl)
                            .build();
                    Timber.v("Calling %s", newReq.urlString());
                    //proceed using our updated request
                } else {
                    newReq = ogReq;
                }
                return chain.proceed(newReq);
            }
        });
        return client;
    }
}
