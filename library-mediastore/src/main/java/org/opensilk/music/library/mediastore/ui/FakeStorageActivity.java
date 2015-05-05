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

package org.opensilk.music.library.mediastore.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.mediastore.R;

/**
 * Created by drew on 5/3/15.
 */
public class FakeStorageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LibraryInfo libraryInfo = new LibraryInfo("0", getString(R.string.mediastore_library_label), null, null);
        Intent i = new Intent().putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
        setResult(RESULT_OK, i);
        finish();
    }
}
