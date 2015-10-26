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

package org.opensilk.music.ui3.profile.bio;

import org.opensilk.music.index.model.BioSummary;
import org.opensilk.music.model.ArtInfo;

import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 10/18/15.
 */
@Module
public class BioScreenModule {
    final BioScreen screen;

    public BioScreenModule(BioScreen screen) {
        this.screen = screen;
    }

    @Provides
    public BioSummary provideSummary() {
        return screen.bioSummary;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideArtinfos() {
        return screen.artInfos;
    }
}
