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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.mediastore.MediaStoreLibraryComponent;
import org.opensilk.music.library.mediastore.R;
import org.opensilk.music.library.mediastore.util.StorageLookup;

import javax.inject.Inject;

import mortar.MortarScope;

/**
 * Created by drew on 11/13/14.
 */
public class StoragePickerActivity extends MortarActivity {

    @Inject StorageLookup mStorageLookup;

    AlertDialog mDialog;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppContextComponent acc = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, MediaStoreLibraryComponent.FACTORY.call(acc));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean wantLightTheme = getIntent().getBooleanExtra(LibraryConstants.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.FoldersThemeTranslucentLight);
        } else {
            setTheme(R.style.FoldersThemeTranslucentDark);
        }

        super.onCreate(savedInstanceState);

        DaggerService.<MediaStoreLibraryComponent>getDaggerComponent(this).inject(this);

        setResult(RESULT_CANCELED, new Intent());

        final String[] storagePaths = mStorageLookup.getStoragePaths();
        final String[] storageLocations;
        if (storagePaths.length == 2) {
            storageLocations = new String[] {
                    getString(R.string.folders_storage_primary),
                    getString(R.string.folders_storage_secondary)
            };
        } else {
            storageLocations = new String[] {
                    getString(R.string.folders_storage_primary)
            };
        }

        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.folders_picker_title)
                .setItems(storageLocations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LibraryInfo libraryInfo = new LibraryInfo(String.valueOf(which), storageLocations[which], null, null);
                        Intent i = new Intent().putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
                        setResult(RESULT_OK, i);
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
