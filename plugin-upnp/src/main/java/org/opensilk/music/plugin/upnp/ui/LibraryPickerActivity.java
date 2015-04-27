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

package org.opensilk.music.plugin.upnp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.plugin.upnp.R;
import org.opensilk.music.plugin.upnp.UpnpServiceService;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by drew on 6/14/14.
 */
public class LibraryPickerActivity extends Activity implements ServiceConnection {

    AndroidUpnpService upnpService;
    ArrayAdapter<DeviceHolder> listAdapter;
    AlertDialog dialog;
    @InjectView(android.R.id.list) ListView listView;
    @InjectView(android.R.id.empty) View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean wantLightTheme = getIntent().getBooleanExtra(OrpheusApi.EXTRA_WANT_LIGHT_THEME, false);
        if (wantLightTheme) {
            setTheme(R.style.AppThemeTranslucentLight);
        } else {
            setTheme(R.style.AppThemeTranslucentDark);
        }

        bindService(new Intent(this, UpnpServiceService.class), this, BIND_AUTO_CREATE);

        setResult(RESULT_CANCELED, new Intent());

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        View view = LayoutInflater.from(this).inflate(R.layout.activity_librarychooser, null);
        ButterKnife.inject(this, view);

        listView.setEmptyView(emptyView);
        listView.setDividerHeight(0);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick((ListView)parent, view, position, id);
            }
        });

        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.select_device)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onCanceled();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onCanceled();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        unbindService(this);
    }

    //@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        dialog.dismiss();

        final DeviceHolder holder = listAdapter.getItem(position);
        LibraryInfo libraryInfo = new LibraryInfo(holder.id, holder.label, null, null);
        Intent i = new Intent()
                .putExtra(OrpheusApi.EXTRA_LIBRARY_ID, holder.id)
                .putExtra(OrpheusApi.EXTRA_LIBRARY_INFO, libraryInfo);
        setResult(RESULT_OK, i);
        finish();
    }


    void onCanceled() {
        finish();
    }

    /*
     * Service Connection
     */

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        upnpService = (AndroidUpnpService) service;
        listAdapter.clear();
        for (RemoteDevice d : upnpService.getRegistry().getRemoteDevices()) {
            listAdapter.add(DeviceHolder.fromRemoteDevice(d));
        }
        upnpService.getRegistry().addListener(registryListener);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        upnpService = null;
        listAdapter.clear();
    }

    /**
     * UpnpRegistry listener
     */
    final RegistryListener registryListener = new DefaultRegistryListener() {
        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            final DeviceHolder holder = DeviceHolder.fromRemoteDevice(device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int position = listAdapter.getPosition(holder);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(holder);
                        listAdapter.insert(holder, position);
                    } else {
                        listAdapter.add(holder);
                    }
                }
            });
        }

        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            final DeviceHolder holder = DeviceHolder.fromRemoteDevice(device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int position = listAdapter.getPosition(holder);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(holder);
                        listAdapter.insert(holder, position);
                    }
                }
            });
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            final DeviceHolder holder = DeviceHolder.fromRemoteDevice(device);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listAdapter.remove(holder);
                }
            });
        }
    };

    static class DeviceHolder {
        final String id;
        final String label;

        DeviceHolder(String id, String label) {
            this.id = id;
            this.label = label;
        }

        static DeviceHolder fromRemoteDevice(RemoteDevice d) {
            final String id = d.getIdentity().getUdn().getIdentifierString();
            final String label = !TextUtils.isEmpty(d.getDetails().getFriendlyName()) ?
                    d.getDetails().getFriendlyName() : d.getDisplayString();
            return new DeviceHolder(id, label);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DeviceHolder)) return false;

            DeviceHolder holder = (DeviceHolder) o;

            if (!id.equals(holder.id)) return false;
//            if (!label.equals(holder.label)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
//            result = 31 * result + label.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
