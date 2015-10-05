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

import com.squareup.okhttp.OkHttpClient;

import org.opensilk.music.artwork.Constants;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Created by drew on 10/4/15.
 */
@Module
public class CoverArtArchiveModule {

    @Provides @Singleton
    public CoverArtArchive provideCoverArtArchive(OkHttpClient client, @Named("CoverArtArchiveEndpoint") String endpoint) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addConverterFactory(new MetadataConverterFactory())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(client)
                .build();
        return retrofit.create(CoverArtArchive.class);
    }

    @Provides @Named("CoverArtArchiveEndpoint")
    public String provideCoverArtArchiveEndpoint() {
        return CoverArtArchive.API_ROOT;
    }

}
