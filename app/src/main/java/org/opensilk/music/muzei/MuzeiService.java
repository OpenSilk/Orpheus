/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.muzei;

import android.net.Uri;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.AppModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.functions.Func2;

/**
 * Created by drew on 4/16/14.
 */
public class MuzeiService extends MuzeiArtSource {
    private static final String TAG = MuzeiService.class.getSimpleName();

    @dagger.Module(addsTo = AppModule.class, injects = MuzeiService.class)
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return EventBus.getDefault();
        }
    }

    public static final String MUZEI_EXTENSION_ENABLED = "is_muzei_enabled";

    @Inject MusicServiceConnection mMusicService;
    @Inject AppPreferences mSettings;

    public MuzeiService() {
        super("Orpheus");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((DaggerInjector) getApplication()).getObjectGraph().plus(new Module()).inject(this);
        mMusicService.bind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMusicService.unbind();
    }

    @Override
    //@DebugLog
    public void onEnabled() {
        // Tells the receiver its ok to send us updates
        // Note: can't simply enable/disable the receiver because
        // muzei detects package changes and doing so will result
        // in an endless loop on enable/disable
        mSettings.putBoolean(MUZEI_EXTENSION_ENABLED, true);
    }

    @Override
    //@DebugLog
    public void onDisabled() {
        mSettings.putBoolean(MUZEI_EXTENSION_ENABLED, false);
    }

    @Override
    //@DebugLog
    protected void onUpdate(int reason) {
        try {
            ArtInfo info = mMusicService.getCurrentArtInfo().toBlocking().first();
            if (info != null) {
                final Uri artworUri = ArtworkProvider.createArtworkUri(info.artistName, info.albumName);
                String[] meta = Observable.zip(mMusicService.getAlbumName(), mMusicService.getArtistName(),
                        new Func2<String, String, String[]>() {
                            @Override
                            public String[] call(String s, String s2) {
                                return new String[] {s, s2};
                            }
                        }).toBlocking().first();
                publishArtwork(new Artwork.Builder()
                        .imageUri(artworUri)
                        .title(meta[0])
                        .byline(meta[1])
                        .build());
            }
        } catch (Exception e) {

        }
    }

}
