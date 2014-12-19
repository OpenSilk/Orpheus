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

package org.opensilk.music.dream;

import org.opensilk.music.AppModule;
import org.opensilk.music.dream.views.ArtOnly;
import org.opensilk.music.dream.views.ArtWithControls;
import org.opensilk.music.dream.views.ArtWithMeta;
import org.opensilk.music.dream.views.VisualizerWave;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;

/**
 * Created by drew on 12/19/14.
 */
@dagger.Module(
        addsTo = AppModule.class,
        injects = {
                DayDreamService.class,
                DreamSettings.class,
                AlternateDreamFragment.class,
                ChooserFragment.class,
                ArtOnly.class,
                ArtWithControls.class,
                ArtWithMeta.class,
                VisualizerWave.class,
        }
)
public class DreamModule {
    @Provides @Singleton @Named("activity")
    public EventBus provideEventBus() {
        return new EventBus();
    }
}
