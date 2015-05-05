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

package org.opensilk.music.plugin.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;

/**
 * Created by drew on 7/18/14.
 */
public class FolderPickerActivity extends AppCompatActivity {

    private String mAuthority;
    private LibraryInfo mLibraryInfo;

    public static Intent buildIntent(Intent parent, Context context,
                                     String authority, LibraryInfo libraryInfo) {
        return new Intent(context, FolderPickerActivity.class)
                .putExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, parent.getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, false))
                .putExtra(LibraryConstants.EXTRA_LIBRARY_AUTHORITY, authority)
                .putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean wantLightTheme = getIntent().getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppThemeDark);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toolbar_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuthority = getIntent().getStringExtra(LibraryConstants.EXTRA_LIBRARY_AUTHORITY);
        mLibraryInfo = getIntent().getParcelableExtra(LibraryConstants.EXTRA_LIBRARY_INFO);

        setResult(RESULT_CANCELED, null);

        if (savedInstanceState == null) {
            pushFolder(mAuthority, mLibraryInfo);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void pushFolder(String authority, LibraryInfo libraryInfo) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, FolderPickerFragment.newInstance(authority, libraryInfo));
        if (!TextUtils.isEmpty(libraryInfo.folderId) && !TextUtils.equals(libraryInfo.folderId, mLibraryInfo.folderId)) {
            ft.addToBackStack(libraryInfo.folderId);
        }
        ft.commit();
    }

    void onFolderSelected(LibraryInfo libraryInfo) {
        Intent i = new Intent().putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
        setResult(RESULT_OK, i);
        finish();
    }
}
