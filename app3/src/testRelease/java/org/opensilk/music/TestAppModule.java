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

package org.opensilk.music;

import android.content.Context;

import org.mockito.Mockito;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.artwork.shared.ArtworkAuthorityModule;
import org.opensilk.music.index.IndexProviderAuthorityModule;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.playback.control.PlaybackController;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 11/16/15.
 */
@Module(
        includes = {
                ArtworkAuthorityModule.class,
                IndexProviderAuthorityModule.class,
        }
)
public class TestAppModule {
    private final App app;

    public TestAppModule(App app) {
        this.app = app;
    }

    @Provides @Singleton @ForApplication
    public App provideApplication() {
        return app;
    }

    @Provides @Singleton @ForApplication
    public Context provideAppContext() {
        return app;
    }

    @Provides @Singleton
    public ArtworkRequestManager provideArtworkRequestManager() {
        return Mockito.mock(ArtworkRequestManager.class);
    }

    @Provides
    public IndexClient provideIndexClient() {
        return Mockito.mock(IndexClient.class);
    }

    @Provides @Singleton
    public PlaybackController providePlaybackController() {
        return Mockito.mock(PlaybackController.class);
    }
}
