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

package org.opensilk.music.index.database;

import android.net.Uri;
import android.os.Build;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.BuildConfig;
import org.opensilk.music.index.IndexTestApplication;
import org.opensilk.music.index.IndexTestComponent;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Created by drew on 9/20/15.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = Build.VERSION_CODES.LOLLIPOP,
        application = IndexTestApplication.class
)
public class DatabaseTest {
    static {
        ShadowLog.stream = System.out;
    }

    IndexDatabase mDb;

    @Before
    public void setup() {
        IndexTestComponent cmpt = DaggerService.getDaggerComponent(RuntimeEnvironment.application);
        mDb = cmpt.indexDatabase();
    }

    @Test
    public void testAddRemoveContainers() {
        Uri uri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo").appendPath("bar").build();
        Uri parentUri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo").build();
        long insId = mDb.addContainer(uri, parentUri);
        long lookId = mDb.hasContainer(uri);
        Assert.assertEquals(insId, lookId);
        for (int ii=0; ii<10; ii++) {
            Uri child = new Uri.Builder().scheme("content").authority("sample")
                    .appendPath("foo").appendPath("bar").appendPath(String.valueOf(ii)).build();
            long iid = mDb.addContainer(child, uri);
            long lid = mDb.hasContainer(child);
            Assert.assertEquals(iid, lid);
        }
        int cnt = mDb.removeContainer(uri);
        Assert.assertEquals(cnt, 11);
    }

    @Test
    public void testDoubleInsert() {
        Uri uri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo2").appendPath("bar").build();
        Uri parentUri = new Uri.Builder().scheme("content").authority("sample").appendPath("foo2").build();
        long insId = mDb.addContainer(uri, parentUri);
        long insId2 =mDb.addContainer(uri, parentUri);
        Assert.assertEquals(insId, insId2);
    }

}
