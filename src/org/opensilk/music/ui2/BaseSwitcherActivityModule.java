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

package org.opensilk.music.ui2;

import android.os.Parcelable;

import org.opensilk.common.mortarflow.AppFlowPresenter;
import org.opensilk.music.ui2.loader.LoaderModule;

import javax.inject.Singleton;

import dagger.Provides;
import flow.Parcer;

/**
 * Created by drew on 4/20/15.
 */
@dagger.Module(
        includes = {
                BaseMortarActivityModule.class,
                LoaderModule.class,
        }, library = true
)
public class BaseSwitcherActivityModule {
    @Provides
    @Singleton
    public Parcer<Object> provideParcer() {
        return new Parcer<Object>() {
            @Override
            public Parcelable wrap(Object instance) {
                return (Parcelable) instance;
            }

            @Override
            public Object unwrap(Parcelable parcelable) {
                return parcelable;
            }
        };
    }

    @Provides
    @Singleton
    public AppFlowPresenter<BaseSwitcherActivity> providePresenter(Parcer<Object> floParcer) {
        return new AppFlowPresenter<>(floParcer);
    }
}
