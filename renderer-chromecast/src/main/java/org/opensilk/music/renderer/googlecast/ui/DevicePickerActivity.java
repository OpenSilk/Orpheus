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

package org.opensilk.music.renderer.googlecast.ui;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteChooserDialog;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.widget.Toast;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.okhttp.OkHttpComponent;
import org.opensilk.music.renderer.googlecast.CastComponent;
import org.opensilk.music.renderer.googlecast.CastRendererService;
import org.opensilk.music.renderer.googlecast.R;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 10/28/15.
 */
public class DevicePickerActivity extends AppCompatActivity implements Dialog.OnDismissListener {

    @Inject MediaRouter mMediaRouter;
    @Inject ConnectivityManager mConnectivityManager;
    @Inject WifiManager mWifiManager;

    MediaRouteChooserDialog mDialog;
    MediaRouterCallback mCallback;

    @Override
    @DebugLog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CastComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        DevicePickerActivityComponent.FACTORY.call(parent).inject(this);

        setResult(RESULT_CANCELED, new Intent());

        //always reset route
        mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        final MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.cast_id)))
                .build();
        mCallback = new MediaRouterCallback();
        mMediaRouter.addCallback(selector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        mDialog = new MediaRouteChooserDialog(this);
        mDialog.setOnDismissListener(this);
        mDialog.setRouteSelector(selector);
        mDialog.show();
    }

    @Override
    @DebugLog
    protected void onDestroy() {
        super.onDestroy();
        mMediaRouter.removeCallback(mCallback);
        mDialog.setDismissMessage(null);
        mDialog.dismiss();
    }

    @Override
    @DebugLog
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    void validateRoute(MediaRouter.RouteInfo route) {
        CastDevice device = CastDevice.getFromBundle(route.getExtras());
        if (device == null || !device.isOnLocalNetwork()) {
            Toast.makeText(DevicePickerActivity.this, "Error must be on same wifi network as cast device", Toast.LENGTH_LONG).show();
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            setResult(RESULT_CANCELED, new Intent());
        } else {
            startService(new Intent(DevicePickerActivity.this, CastRendererService.class).setAction("route_selected"));
            setResult(RESULT_OK, new Intent()
                    .setComponent(new ComponentName(DevicePickerActivity.this, CastRendererService.class)));
        }
    }

    class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            validateRoute(route);
        }
    }
}
