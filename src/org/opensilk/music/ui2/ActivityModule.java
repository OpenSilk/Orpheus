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

package org.opensilk.music.ui2;

import com.google.gson.Gson;
import com.squareup.otto.Bus;

import org.opensilk.common.flow.GsonParcer;
import org.opensilk.common.mortar.PauseAndResumeModule;
import org.opensilk.common.mortar.ScreenScoper;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import flow.Parcer;

/**
 * Created by drew on 10/23/14.
 */
@Module(
        includes = {
                ActionBarOwner.Module.class,
                PauseAndResumeModule.class,
        },
        library = true,
        complete = false
)
public class ActivityModule {
    @Provides @Singleton @Named("activity")
    public EventBus provideEventBus() {
        return new EventBus();
    }

    @Provides @Singleton
    public Parcer<Object> provideParcer(Gson gson) {
        return new GsonParcer<>(gson);
    }
}
