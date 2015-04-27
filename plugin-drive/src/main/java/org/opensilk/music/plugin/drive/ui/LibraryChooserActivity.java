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
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.plugin.drive.R;
import org.opensilk.music.plugin.drive.util.AuthTest;
import org.opensilk.music.plugin.drive.util.DriveHelper;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Subscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 6/15/14.
 */
public class LibraryChooserActivity extends Activity {

    public static final int REQUEST_ACCOUNT_PICKER = RESULT_FIRST_USER << 1;
    public static final int REQUEST_AUTH_APPROVAL = RESULT_FIRST_USER << 2;

    @Inject DriveHelper mDriveHelper;

    private String mAccountName;
    private Subscription mAuthTestSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((DaggerInjector) getApplication()).inject(this);

        boolean wantLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeTranslucentLight);
        } else {
            setTheme(R.style.AppThemeTranslucentDark);
        }

        // hack, no rotating, background task will leak activity
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        setResult(RESULT_CANCELED);

        if (savedInstanceState == null) {
            Intent i = AccountManager.newChooseAccountIntent(
                    null, null, new String[]{"com.google"}, true, null, null, null, null);
            startActivityForResult(i, REQUEST_ACCOUNT_PICKER);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isSubscribed(mAuthTestSubscription)) mAuthTestSubscription.unsubscribe();
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
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startTest() {
        // show progress
        ProgressFragment.newInstance().show(getFragmentManager(), "progress");
        // start the test
        mAuthTestSubscription = AuthTest.create(mDriveHelper.getSession(mAccountName),
                new SimpleObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean aBoolean) {
                        finishSuccess();
                    }
                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof UserRecoverableAuthIOException) {
                            Intent intent = ((UserRecoverableAuthIOException) e).getIntent();
                            if (intent != null) {
                                startActivityForResult(intent, REQUEST_AUTH_APPROVAL);
                                return;
                            }
                        }
                        finishFailure();
                    }
                });
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

    public static class ProgressFragment extends DialogFragment {

        public static ProgressFragment newInstance() {
            return new ProgressFragment();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setCancelable(false);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.authorizing));
            return mProgressDialog;
        }
    }

}
