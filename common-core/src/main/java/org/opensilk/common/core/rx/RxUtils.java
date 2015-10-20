/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.core.rx;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by drew on 10/16/14.
 */
public class RxUtils {

    private RxUtils(){}

    public static boolean notSubscribed(Subscription subscription) {
        return subscription == null || subscription.isUnsubscribed();
    }

    public static boolean isSubscribed(Subscription subscription) {
        return subscription != null && !subscription.isUnsubscribed();
    }

    public static boolean unsubscribe(Subscription subscription) {
        if (isSubscribed(subscription)) {
            subscription.unsubscribe();
            return true;
        }
        return false;
    }

    /**
     * Slightly less typing
     */
    public static <T> Observable<T> observeOnMain(Observable<T> observable) {
        return observable.observeOn(AndroidSchedulers.mainThread());
    }
}
