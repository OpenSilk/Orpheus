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

package org.opensilk.music.plugin.drive.util;

import android.os.Bundle;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensilk.music.api.callback.Result;
import org.opensilk.music.api.exception.ParcelableException;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by drew on 11/11/14.
 */
@RunWith(RobolectricTestRunner.class)
@Config( manifest = Config.NONE)
public class RequestCacheTest {

    static class TestResult extends Result.Stub {
        List<Bundle> list;
        Bundle token;
        @Override
        public void onNext(List<Bundle> items, Bundle paginationBundle) throws RemoteException {
            list = items;
            token = paginationBundle;
        }

        @Override
        public void onError(ParcelableException e) throws RemoteException {

        }
    }

    final String key1 = "key1";

    final List<Bundle> list1 = new ArrayList<>();

    final TestResult result1 = new TestResult();

    final RequestCache cache = new RequestCache();

    @Before
    public void setUp() {
        for (int ii=0 ; ii < 50 ; ii++) {
            Bundle b = new Bundle();
            b.putInt("num", ii);
            list1.add(b);
        }
        cache.put(key1, list1);
    }

    @Test
    public void testPagination() {
        //First page
        cache.get(key1, 0, 20, result1);
        assertThat(result1.list.size() == 20);
        //Next page
        cache.get(key1, result1.token.getInt("startpos"), 20, result1);
        assertThat(result1.list.size() == 20);
        assertThat(result1.list.get(0).getInt("num") == 21);
        assertThat(result1.list.get(result1.list.size()-1).getInt("num") == 40);
        //Next page
        cache.get(key1, result1.token.getInt("startpos"), 20, result1);
        assertThat(result1.list.size() == 10);
        assertThat(result1.token == null);
        assertThat(result1.list.get(0).getInt("num") == 41);
        assertThat(result1.list.get(result1.list.size()-1).getInt("num") == 49);
        //All but one
        cache.get(key1, 0, 49, result1);
        assertThat(result1.list.size() == 49);
        assertThat(result1.list.get(0).getInt("num") == 0);
        assertThat(result1.list.get(result1.list.size()-1).getInt("num") == 48);
        //Next page (last item)
        cache.get(key1, result1.token.getInt("startpos"), 20, result1);
        assertThat(result1.list.size() == 1);
        assertThat(result1.token == null);
        assertThat(result1.list.get(0).getInt("num") == 49);
    }

    @Test
    public void testOutOfBounds() {
        // clip results
        cache.get(key1, 48, 20, result1);
        assertThat(result1.list.size() == 2);
        assertThat(result1.token == null);
        assertThat(result1.list.get(0).getInt("num") == 48);
        //last item
        cache.get(key1, 49, 20, result1);
        assertThat(result1.list.size() == 1);
        assertThat(result1.token == null);
        assertThat(result1.list.get(0).getInt("num") == 49);
        //start out of bounds
        cache.get(key1, 50, 20, result1);
        assertThat(result1.list.isEmpty());
        assertThat(result1.token == null);
    }

}
