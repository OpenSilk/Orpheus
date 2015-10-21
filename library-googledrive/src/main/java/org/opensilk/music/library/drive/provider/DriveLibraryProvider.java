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

package org.opensilk.music.library.drive.provider;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.drive.client.DriveClient;
import org.opensilk.music.library.drive.client.DriveClientModule;
import org.opensilk.music.library.drive.ui.ChooserActivity;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscriber;

import static org.opensilk.music.library.drive.Constants.DEFAULT_ROOT_FOLDER;
import static org.opensilk.music.library.drive.provider.DriveLibraryDB.SCHEMA.ACCOUNT;
import static org.opensilk.music.library.drive.provider.DriveLibraryUris.M_FILE;
import static org.opensilk.music.library.drive.provider.DriveLibraryUris.M_FOLDER;
import static org.opensilk.music.library.drive.provider.DriveLibraryUris.M_ROOT_FOLDER;
import static org.opensilk.music.library.drive.provider.DriveLibraryUris.extractAccount;
import static org.opensilk.music.library.drive.provider.DriveLibraryUris.extractId;

/**
 * Created by drew on 4/28/15.
 */
public class DriveLibraryProvider extends LibraryProvider {

    public static final String INSERT_ACCOUNT = "drive_insert_account";
    public static final String INVALIDATE_ACCOUNT = "drive_invalidate_account";

    @Inject @Named("driveLibraryAuthority") String mAuthority;
    @Inject DriveLibraryDB mDB;

    DriveLibraryComponent mComponent;
    UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        AppContextComponent parent = DaggerService.getDaggerComponent(getContext());
        mComponent = DriveLibraryComponent.FACTORY.call(parent, new DriveLibraryModule());
        mComponent.inject(this);
        mUriMatcher = DriveLibraryUris.matcher(mAuthority);
        return super.onCreate();
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                .setLoginComponent(new ComponentName(getContext(), ChooserActivity.class))
                .build();
    }

    @Override
    protected String getAuthority() {
        return mAuthority;
    }

    @Override
    protected Observable<Model> getListObjsObservable(Uri uri, Bundle args) {
        final DriveClient client = mComponent.driveClientComponent(
                new DriveClientModule(extractAccount(uri))).client();
        switch (mUriMatcher.match(uri)) {
            case M_ROOT_FOLDER: {
                return client.listFolder(DEFAULT_ROOT_FOLDER);
            }
            case M_FOLDER: {
                return client.listFolder(extractId(uri));
            }
            default:
                return super.getListObjsObservable(uri, args);
        }
    }

    @Override
    protected Observable<Model> getGetObjObservable(Uri uri, Bundle args) {
        final DriveClient client = mComponent.driveClientComponent(
                new DriveClientModule(extractAccount(uri))).client();
        switch (mUriMatcher.match(uri)) {
            case M_FOLDER:
            case M_FILE: {
                return client.getFile(extractId(uri));
            }
            default:
                return super.getGetObjObservable(uri, args);
        }
    }

    @Override
    protected Observable<Container> getListRootsObservable(Uri uri, Bundle args) {
        return Observable.create(new Observable.OnSubscribe<Container>() {
            @Override
            public void call(Subscriber<? super Container> subscriber) {
                List<Container> accounts = getAccounts();
                if (accounts.isEmpty()) {
                    subscriber.onError(new LibraryException(LibraryException.Kind.AUTH_FAILURE, null));
                } else {
                    for (Container account: accounts) {
                        subscriber.onNext(account);
                    }
                    subscriber.onCompleted();
                }
            }
        });
    }

    @Override
    protected Bundle callCustom(String method, String arg, Bundle extras) {
        switch (method) {
            case INSERT_ACCOUNT: {
                LibraryExtras.Builder ok = LibraryExtras.b();
                ContentValues cv = new ContentValues(2);
                cv.put(ACCOUNT.ACCOUNT, BundleHelper.getString(extras));
                cv.put(ACCOUNT.INVALID, 0);
                long id = mDB.getWritableDatabase().insertWithOnConflict(
                        ACCOUNT.TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                ok.putOk(id > 0);
                return ok.get();
            }
            case INVALIDATE_ACCOUNT: {
                LibraryExtras.Builder ok = LibraryExtras.b();
                ContentValues cv = new ContentValues(2);
                cv.put(ACCOUNT.INVALID, 1);
                int num = mDB.getWritableDatabase().update(
                        ACCOUNT.TABLE, cv, ACCOUNT.ACCOUNT + "=?",
                        new String[]{BundleHelper.getString(extras)});
                ok.putOk(num > 0);
                return ok.get();
            }
            default:
                return super.callCustom(method, arg, extras);
        }
    }

    static final String[] accountsProj = new String[] {
            ACCOUNT.ACCOUNT,
    };
    static final String getAccountsSel = ACCOUNT.INVALID + "!=1";
    public List<Container> getAccounts() {
        List<Container> accounts = new ArrayList<>();
        Cursor c = null;
        try {
            c = mDB.getReadableDatabase().query(ACCOUNT.TABLE,
                    accountsProj, getAccountsSel, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    accounts.add(Folder.builder()
                            .setUri(DriveLibraryUris.rootFolder(mAuthority, c.getString(0)))
                            .setParentUri(LibraryUris.rootUri(mAuthority))
                            .setName(c.getString(0))
                            .build());
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return accounts;
    }

}
