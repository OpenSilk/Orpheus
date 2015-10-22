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

import android.content.Context;
import android.content.Intent;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import org.opensilk.music.library.drive.Constants;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Created by drew on 10/20/15.
 */
public class DriveAuthService {

    public static final String SERVICE_NAME = DriveAuthService.class.getName();

    final Context context;
    final GoogleAccountCredential credential;

    private BehaviorSubject<String> tokenSubject;

    public DriveAuthService(Context context) {
        this.context = context;
        this.credential = GoogleAccountCredential.usingOAuth2(context, Constants.SCOPES);
    }

    @SuppressWarnings("ResourceType")
    public static DriveAuthService getService(Context context) {
        return (DriveAuthService) context.getSystemService(SERVICE_NAME);
    }

    public Intent getAccountChooserIntent() {
        return credential.newChooseAccountIntent();
    }

    public Subscription getToken(String account, Subscriber<String> subscriber, boolean force) {
        if (tokenSubject == null || force) {
            credential.setSelectedAccountName(account);
            tokenSubject = BehaviorSubject.create();
            Observable.create(
                    new Observable.OnSubscribe<String>() {
                        @Override
                        public void call(Subscriber<? super String> subscriber) {
                            try {
                                Timber.d("Fetching token");
                                String token = credential.getToken();
                                Timber.d("Found token");
                                subscriber.onNext(token);
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tokenSubject);
        }
        return tokenSubject.subscribe(subscriber);
    }

    public boolean isFetchingToken() {
        return tokenSubject != null;
    }
}
