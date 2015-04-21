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

package org.opensilk.iab.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

/**
 * Created by drew on 4/21/15.
 */
@Singleton
public class DonateManager {
    public static final boolean T = false; // for testing iab;
    public static final boolean D = false;

    private final DonateProvider provider;
    private final IabSettings settings;

    @Inject
    public DonateManager(
            DonateProvider provider,
            IabSettings settings
    ) {
        this.provider = provider;
        this.settings = settings;
    }

    @Nullable
    public Subscription onCreate(Activity activity) {
        settings.incrementAppLaunchCount();
        if (T || settings.shouldBother()) {
            final WeakReference<Activity> wActivity = new WeakReference<>(activity);
            return AndroidObservable.bindActivity(activity, provider.hasDonated())
                    .subscribe(new Action1<Boolean>() {
                                   @Override
                                   public void call(Boolean donated) {
                                       Activity a = wActivity.get();
                                       if (!donated && a != null) {
                                           showDonateDialog(a);
                                       }
                                   }
                               }, new Action1<Throwable>() {
                                   @Override
                                   public void call(Throwable throwable) {
                                       Log.w("IabManager", "query failure: cause=" + throwable);
                                   }
                               }
                    );
        }
        return null;
    }

    public void launchDonateActivity(Activity context) {
        provider.launchDonateActivity(context);
    }

    void showDonateDialog(final Activity context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.iab_donate_dialog_title)
                .setMessage(R.string.iab_donate_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchDonateActivity(context);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


}
