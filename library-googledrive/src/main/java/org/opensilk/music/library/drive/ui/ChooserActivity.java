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

package org.opensilk.music.library.drive.ui;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarActivity;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.library.drive.R;
import org.opensilk.music.library.drive.provider.DriveLibraryProvider;
import org.opensilk.music.library.drive.provider.DriveLibraryUris;
import org.opensilk.music.library.provider.LibraryExtras;

import javax.inject.Inject;
import javax.inject.Named;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by drew on 6/15/14.
 */
public class ChooserActivity extends MortarActivity {

    public static final int REQUEST_ACCOUNT_PICKER = 1001;
    public static final int REQUEST_AUTH_APPROVAL = 1002;

    @Inject @Named("driveLibraryAuthority") String mAuthority;

    private String mAccountName;
    DriveAuthService mAuthService;
    Subscription authSubscription;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppContextComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        ChooserActivityComponent cmp = ChooserActivityComponent.FACTORY.call(parent);
        builder.withService(DaggerService.DAGGER_SERVICE, cmp)
                .withService(DriveAuthService.SERVICE_NAME, new DriveAuthService(getApplicationContext()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //We Ignore light/dark theme suggestion since google login starts dark then goes to light so fuck it.
        super.onCreate(savedInstanceState);

        ChooserActivityComponent cmp = DaggerService.getDaggerComponent(this);
        cmp.inject(this);

        setResult(RESULT_CANCELED);

        mAuthService = DriveAuthService.getService(this);

        if (savedInstanceState == null) {
            Intent i = mAuthService.getAccountChooserIntent();
            startActivityForResult(i, REQUEST_ACCOUNT_PICKER);
        } else {
            mAccountName = savedInstanceState.getString("account");
            if (mAuthService.isFetchingToken()) {
                authSubscription = mAuthService.getToken(new TokenSubscriber(), false);
            }
        }

        int error;
        if (ConnectionResult.SUCCESS !=
                (error = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this))) {
            if (!GoogleApiAvailability.getInstance().isUserResolvableError(error)) {
                finishFailure();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("account", mAccountName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxUtils.unsubscribe(authSubscription);
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
                    fetchToken(false);
                } else {
                    finishFailure();
                }
                break;
            case REQUEST_AUTH_APPROVAL:
                if (resultCode == RESULT_OK) {
                    //retry
                    fetchToken(true);
                } else {
                    finishFailure();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void finishSuccess() {
        //add the account to the db
        Bundle reply = getContentResolver().call(DriveLibraryUris.call(mAuthority),
                DriveLibraryProvider.INSERT_ACCOUNT, null, BundleHelper.b().putString(mAccountName).get());
        if (LibraryExtras.getOk(reply)) {
            setResult(RESULT_OK, new Intent());
            finish();
        } else {
            finishFailure();
        }
    }

    private void finishFailure() {
        Toast.makeText(this, R.string.drive_msg_unable_to_authorize, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void fetchToken(boolean retry) {
        ProgressFragment.newInstance().show(getFragmentManager(), ProgressFragment.TAG);
        authSubscription = mAuthService.getToken(new TokenSubscriber(), retry);
    }

    class TokenSubscriber extends Subscriber<String> {
        @Override
        public void onCompleted() {
            //never called
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof UserRecoverableAuthIOException) {
                startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(), REQUEST_AUTH_APPROVAL);
            } else if (e instanceof UserRecoverableAuthException) {
                startActivityForResult(((UserRecoverableAuthException) e).getIntent(), REQUEST_AUTH_APPROVAL);
            } else {
                finishFailure();
            }
        }

        @Override
        public void onNext(String s) {
            finishSuccess();
        }
    }

}
