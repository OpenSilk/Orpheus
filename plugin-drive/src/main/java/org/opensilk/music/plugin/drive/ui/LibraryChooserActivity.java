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

package org.opensilk.music.plugin.drive.ui;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.opensilk.common.core.app.MortarActivity;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.plugin.drive.GlobalComponent;
import org.opensilk.music.plugin.drive.R;

import hugo.weaving.DebugLog;
import mortar.MortarScope;

/**
 * Created by drew on 6/15/14.
 */
public class LibraryChooserActivity extends MortarActivity implements AuthTestFragment.OnTestResults {

    public static final int REQUEST_ACCOUNT_PICKER = 1001;
    public static final int REQUEST_AUTH_APPROVAL = 1002;

    private String mAccountName;
    private AuthTestFragment mAuthTestFragment;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        builder.withService(DaggerService.DAGGER_SERVICE, ActivityComponent.FACTORY.call(
                DaggerService.<GlobalComponent>getDaggerComponent(getApplicationContext())));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean wantLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeTranslucentLight);
        } else {
            setTheme(R.style.AppThemeTranslucentDark);
        }

        setResult(RESULT_CANCELED);

        if (savedInstanceState == null) {
            Intent i = AccountManager.newChooseAccountIntent(
                    null, null, new String[]{"com.google"}, true, null, null, null, null);
            startActivityForResult(i, REQUEST_ACCOUNT_PICKER);
        } else {
            if (savedInstanceState.getBoolean("istesting", false)) {
                mAuthTestFragment = (AuthTestFragment) getFragmentManager().findFragmentByTag(AuthTestFragment.TAG);
                if (mAuthTestFragment != null) {//Shouldnt happen
                    mAuthTestFragment.setListener(this);
                }
            }
            mAccountName = savedInstanceState.getString("account");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("account", mAccountName);
        outState.putBoolean("istesting", mAuthTestFragment != null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuthTestFragment != null) {
            mAuthTestFragment.setListener(null);
        }
    }

    @Override
    @DebugLog
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK) {
                    mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    // After we select an account we still need to authorize ourselves
                    // for drive access.
                    startTest();
                } else {
                    finishFailure();
                }
                break;
            case REQUEST_AUTH_APPROVAL:
                if (resultCode == RESULT_OK) {
                    finishSuccess();
                } else {
                    finishFailure();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startTest() {
        // show progress
        ProgressFragment.newInstance().show(getFragmentManager(), ProgressFragment.TAG);
        // Add tester fragment. It starts the test;
        mAuthTestFragment = AuthTestFragment.newInstance(mAccountName);
        mAuthTestFragment.setListener(this);
        getFragmentManager().beginTransaction()
                .add(mAuthTestFragment, AuthTestFragment.TAG)
                .commit();
    }

    @Override
    @DebugLog
    public void onAuthTestSuccess() {
        finishSuccess();
    }

    @Override
    @DebugLog
    public void onAuthTestFailure(Throwable e) {
        if (e instanceof UserRecoverableAuthIOException) {
            Intent intent = ((UserRecoverableAuthIOException) e).getIntent();
            if (intent != null) {
                startActivityForResult(intent, REQUEST_AUTH_APPROVAL);
                return;
            }
        }
        finishFailure();
    }

    private void finishSuccess() {
        LibraryInfo libraryInfo = new LibraryInfo(mAccountName, mAccountName, null, null);
        Intent i = new Intent()
                .putExtra(OrpheusApi.EXTRA_LIBRARY_ID, mAccountName)
                .putExtra(OrpheusApi.EXTRA_LIBRARY_INFO, libraryInfo);
        setResult(RESULT_OK, i);
        finish();
    }

    private void finishFailure() {
        setResult(RESULT_CANCELED);
        finish();
    }

}
