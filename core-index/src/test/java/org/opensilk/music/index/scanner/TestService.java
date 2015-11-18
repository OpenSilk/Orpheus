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

import android.net.Uri;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.List;

import mortar.MortarScope;

/**
 * Created by drew on 11/16/15.
 */
public class TestService extends ScannerService {
    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        IndexComponent acc = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                DaggerTestComponent.builder()
                        .indexComponent(acc)
                        .testModule(new TestModule(this))
                        .build());
    }

    @Override
    protected List<Bundleable> getChildren(Uri uri) {
        if (TestData.URI_FOLDER1.equals(uri)) {
            return TestData.TRACKS_FOLDER1;
        } else {
            throw new IllegalArgumentException("unknown uri " + uri);
        }
    }
}
