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

import android.content.Intent;
import android.os.Build;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.Preconditions;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.model.Album;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowService;
import org.robolectric.util.ServiceController;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by drew on 9/16/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class TestScannerService {

    static {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testServiceStarts() {
        RuntimeEnvironment.application.startService(new Intent(RuntimeEnvironment.application, ScannerService.class));
    }

    @Test
    public void testlookup() {
        ServiceController<ScannerService> controller = Robolectric.buildService(ScannerService.class);
        controller.create();
        ScannerService service = controller.get();
        long id = service.lookupArtistInfo("foxes");
        Assert.assertNotSame(id, -1);
        long id2 = service.lookupAlbumInfo("foxes", "glorious", id);
        Assert.assertNotSame(id2, -1);
        long id3 =service.checkArtist("foxes");
        Assert.assertNotSame(id3, -1);
        long id4 = service.checkAlbum("foxes", "glorious");
        Assert.assertNotSame(id4, -1);
        IndexComponent acc = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
        IndexDatabase db = acc.indexDatabase();
        List<Album> albums = db.getAlbums(AlbumSortOrder.A_Z);
        Assertions.assertThat(albums.isEmpty()).isFalse();
    }

}
