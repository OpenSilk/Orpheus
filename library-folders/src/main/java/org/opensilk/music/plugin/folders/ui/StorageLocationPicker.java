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

package org.opensilk.music.plugin.folders.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.plugin.folders.R;
import org.opensilk.music.plugin.folders.util.FileUtil;

/**
 * Created by drew on 11/13/14.
 */
public class StorageLocationPicker extends Activity {

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean wantLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.FoldersThemeTranslucentLight);
        } else {
            setTheme(R.style.FoldersThemeTranslucentDark);
        }

        setResult(RESULT_CANCELED, new Intent());

        final String[] storageLocations;
        if (FileUtil.SECONDARY_STORAGE_DIR != null) {
            storageLocations = new String[] {
                    getString(R.string.folders_storage_primary),
                    getString(R.string.folders_storage_secondary)
            };
        } else {
            storageLocations = new String[] {
                    getString(R.string.folders_storage_primary)
            };
        }

        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.folders_picker_title)
                .setItems(storageLocations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LibraryInfo libraryInfo = new LibraryInfo(String.valueOf(which), storageLocations[which], null, null);
                        Intent i = new Intent()
                                .putExtra(OrpheusApi.EXTRA_LIBRARY_INFO, libraryInfo);
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
}
