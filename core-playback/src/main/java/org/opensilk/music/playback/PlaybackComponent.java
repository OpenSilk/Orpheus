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

package org.opensilk.music.playback;

import android.content.Context;

import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.music.index.client.IndexClientModule;
import org.opensilk.music.playback.service.PlaybackServiceComponent;
import org.opensilk.music.playback.service.PlaybackServiceModule;

import javax.inject.Singleton;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 9/26/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                PlaybackModule.class,
                IndexClientModule.class
        }
)
public interface PlaybackComponent {
    Func1<Context, PlaybackComponent> FACTORY =
            new Func1<Context, PlaybackComponent>() {
                @Override
                public PlaybackComponent call(Context context) {
                    return DaggerPlaybackComponent.builder()
                            .appContextModule(new AppContextModule(context))
                            .build();
                }
            };
    PlaybackServiceComponent playbackServiceComponent(PlaybackServiceModule module);
}
