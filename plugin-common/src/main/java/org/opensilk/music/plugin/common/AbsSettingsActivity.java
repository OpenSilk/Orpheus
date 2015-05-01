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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;

import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.music.library.LibraryConstants;

/**
 * Created by drew on 7/19/14.
 */
public abstract class AbsSettingsActivity extends MortarActivity {

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

        String libraryId = "dr3wsuth3rland@gmail.com";// getIntent().getStringExtra(LibraryConstants.EXTRA_LIBRARY_ID);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, getSettingsFragment(libraryId), "settings")
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            finish();
        }
    }

    protected abstract Fragment getSettingsFragment(String libraryId);

    public static void showLicences(Activity activity) {
        new LicensesDialog().show(activity.getFragmentManager(), "licenses");
    }

    public static class LicensesDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about_licenses)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }

}
