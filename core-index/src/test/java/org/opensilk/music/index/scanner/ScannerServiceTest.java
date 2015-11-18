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

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.library.internal.BundleableSubscriber;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryMethods;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

/**
 * Created by drew on 9/16/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class ScannerServiceTest {

    TestService scannerService;
    ContentResolver contentResolver;
    IndexDatabase indexDatabase;

    @Before
    public void setup() {
        scannerService = Robolectric.buildService(TestService.class).attach().create().get();
        contentResolver = RuntimeEnvironment.application.getContentResolver();
        IndexComponent cmp = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
        indexDatabase = cmp.indexDatabase();
    }

    @Test
    public void testScanContainer() throws Exception {
        Container c = Folder.builder()
                .setUri(TestData.URI_FOLDER1)
                .setParentUri(Uri.parse("content://foo/root"))
                .setName("folder1")
                .build();
        Intent i = new Intent().putExtra(ScannerService.EXTRA_LIBRARY_EXTRAS, LibraryExtras.b()
                .putBundleable(c).get());
        scannerService.onHandleIntent(i);

        Assertions.assertThat(indexDatabase.findTopLevelContainers(null).size()).isEqualTo(1);

        Assertions.assertThat(scannerService.numTotal.get()).isEqualTo(5);
        Assertions.assertThat(scannerService.numProcessed.get()).isEqualTo(5);
        Assertions.assertThat(scannerService.numError.get()).isEqualTo(0);


        List<Track> insertedTracks = indexDatabase.getTracks(null);
        Assertions.assertThat(insertedTracks.size()).isEqualTo(5);
    }

}
