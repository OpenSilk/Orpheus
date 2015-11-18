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

package org.opensilk.music.index.scanner;

import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 11/17/15.
 */
@Module
public class TestModule {

    final TestService service;

    public TestModule(TestService service) {
        this.service = service;
    }

    @Provides
    @ScannerScope
    public MetaExtractor provideMetaExtractor() {
        return new MetaExtractor() {
            @Nullable @Override
            public Metadata extractMetadata(Track.Res res) {
                if (StringUtils.contains(res.getUri().toString(), "folder1")) {
                    return TestData.TRACK_META_FOLDER1.get(res.getUri());
                } else {
                    throw new IllegalArgumentException("Unknown uri" + res.getUri());
                }
            }
        };
    }

    @Provides
    public ScannerService provideService() {
        return service;
    }
}
