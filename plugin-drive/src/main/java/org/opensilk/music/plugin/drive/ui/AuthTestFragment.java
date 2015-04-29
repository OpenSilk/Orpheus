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

package org.opensilk.music.plugin.drive.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.plugin.drive.SessionFactory;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 4/28/15.
 */
public class AuthTestFragment extends Fragment implements Observer<Boolean> {
    public static final String TAG = AuthTestFragment.class.getSimpleName();

    public static AuthTestFragment newInstance(String accountName) {
        AuthTestFragment f = new AuthTestFragment();
        Bundle b = new Bundle();
        b.putString("account", accountName);
        f.setArguments(b);
        return f;
    }

    public interface OnTestResults {
        void onAuthTestSuccess();
        void onAuthTestFailure(Throwable e);
    }

    OnTestResults mListener;

    void setListener(OnTestResults o) {
        mListener = o;
        if (o != null && completed) {
            if (error != null) {
                o.onAuthTestFailure(error);
            } else {
                o.onAuthTestSuccess();
            }
        }
    }

    boolean completed;
    Throwable error;

    @Override
    @DebugLog
    public void onCompleted() {
        completed = true;
        if (mListener != null) {
            mListener.onAuthTestSuccess();
        }
    }

    @Override
    @DebugLog
    public void onError(Throwable e) {
        completed = true;
        error = e;
        if (mListener != null) {
            mListener.onAuthTestFailure(e);
        }
    }

    @Override
    public void onNext(Boolean aBoolean) {
    }

    @Inject SessionFactory mSessionFactory;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        DaggerService.<ActivityComponent>getDaggerComponent(activity).inject(this);
        setRetainInstance(true);
    }

    Observable<Boolean> mObservable;
    Subscription mSubscription;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mObservable == null) {
            mObservable = Observable.create(
                    new Observable.OnSubscribe<Boolean>() {
                        @Override
                        public void call(Subscriber<? super Boolean> subscriber) {
                            try {
//                                mSessionFactory.getSession(getArguments().getString("account"))
//                                        .getDrive()
//                                        .files()
//                                        .list()
//                                        .setFields("items/id")
//                                        .setMaxResults(1)
//                                        .execute();
                                mSessionFactory.getSession(getArguments().getString("account"))
                                        .getCredential().getToken();
                                //if (subscriber.isUnsubscribed()) return;
                                //subscriber.onNext(true);
                                subscriber.onCompleted();
                            } catch (Exception e) {
                                //if (subscriber.isUnsubscribed()) return;
                                subscriber.onError(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
            mSubscription = AndroidObservable.bindFragment(this, mObservable).subscribe(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }
}
