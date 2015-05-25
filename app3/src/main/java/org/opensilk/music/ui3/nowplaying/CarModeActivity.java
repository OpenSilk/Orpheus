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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.WindowManager;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.ui3.MusicActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import mortar.MortarScope;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 5/24/15.
 */
public class CarModeActivity extends NowPlayingActivity {

    public static void startSelf(Context context) {
        Intent i = new Intent(context, CarModeActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        AppComponent appComponent = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE,
                CarModeActivityComponent.FACTORY.call(appComponent));
    }

    @Override
    protected void performInjection() {
        CarModeActivityComponent component = DaggerService.getDaggerComponent(this);
        component.inject(this);
    }

    @Override
    protected void setupView() {
        setContentView(R.layout.activity_carmode);
        ButterKnife.inject(this);
    }

    Subscription mChargingSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscribeChargingState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChargingSubscription != null) {
            mChargingSubscription.unsubscribe();
            mChargingSubscription = null;
        }
    }

    void subscribeChargingState() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mChargingSubscription = AndroidObservable.bindActivity(this, AndroidObservable.fromBroadcast(this, filter))
                .subscribe(new Action1<Intent>() {
                               @Override
                               public void call(Intent intent) {
                                   int status = intent != null ? intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) : 0;
                                   Timber.d("received BATTERY_CHANGED plugged=%s", status != 0);
                                   if (status != 0) {
                                       getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                   } else {
                                       getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                   }
                               }
                           }
                );
    }

}
