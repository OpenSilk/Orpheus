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

package org.opensilk.music.ui2.library;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.RemoteException;

import org.opensilk.music.R;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.OrpheusApi.Ability;
import org.opensilk.music.api.RemoteLibrary;
import org.opensilk.music.api.meta.LibraryInfo;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.event.StartActivityForResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by drew on 11/4/14.
 */
public class PluginActionBarConfig {

    final Context appContext;
    final PluginInfo pluginInfo;
    final LibraryInfo libraryInfo;
    final int capabilities;

    public PluginActionBarConfig(Context appContext, PluginInfo pluginInfo, LibraryInfo libraryInfo, int capabilities) {
        this.appContext = appContext;
        this.pluginInfo = pluginInfo;
        this.libraryInfo = libraryInfo;
        this.capabilities = capabilities;
    }

    ActionBarOwner.MenuConfig createMenuConfig(){
        List<Integer> menus = new ArrayList<>();
        List<ActionBarOwner.CustomMenuItem> customMenus = new ArrayList<>();

        // search
        if (hasAbility(Ability.SEARCH)) {
            menus.add(R.menu.search);
        }

        Resources res = null;
        try {
            res = appContext.getPackageManager()
                    .getResourcesForApplication(pluginInfo.componentName.getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {}

        if (res != null) {
            // device selection
            try {
                String change = res.getString(res.getIdentifier("menu_change_source",
                        "string", pluginInfo.componentName.getPackageName()));
                customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_change_source, change));
            } catch (Resources.NotFoundException ignored) {
                menus.add(R.menu.change_source);
            }
            // library settings
            if (hasAbility(Ability.SETTINGS)) {
                try {
                    String settings = res.getString(res.getIdentifier("menu_library_settings",
                            "string", pluginInfo.componentName.getPackageName()));
                    customMenus.add(new ActionBarOwner.CustomMenuItem(R.id.menu_library_settings, settings));
                } catch (Resources.NotFoundException ignored) {
                    menus.add(R.menu.library_settings);
                }
            }
        } else {
            menus.add(R.menu.change_source);
            if (hasAbility(Ability.SETTINGS)) {
                menus.add(R.menu.library_settings);
            }
        }

        int[] menusArray;
        if (!menus.isEmpty()) {
            menusArray = toArray(menus);
        } else {
            menusArray = new int[0];
        }
        ActionBarOwner.CustomMenuItem[] customMenuArray;
        if (!customMenus.isEmpty()) {
            customMenuArray = customMenus.toArray(new ActionBarOwner.CustomMenuItem[customMenus.size()]);
        } else {
            customMenuArray = new ActionBarOwner.CustomMenuItem[0];
        }

        return new ActionBarOwner.MenuConfig(createMenuActionHandler(), menusArray, customMenuArray);
    }

    Func1<Integer, Boolean> createMenuActionHandler() {
        return new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer integer) {
                switch (integer) {
                    case R.id.menu_search:
                        return true;
                    case R.id.menu_change_source:
//                        settings.clearDefaultSource();
                        //TODO reset flow
                        return true;
                    case R.id.menu_library_settings:
//                        connectionManager.bind(pluginInfo.componentName).subscribe(new Action1<RemoteLibrary>() {
//                            @Override
//                            public void call(RemoteLibrary remoteLibrary) {
//                                try {
//                                    Intent i = new Intent();
//                                    remoteLibrary.getSettingsIntent(i);
//                                    if (i.getComponent() != null) {
//                                        i.putExtra(OrpheusApi.EXTRA_LIBRARY_ID, libraryIdentity);
//                                        bus.post(new StartActivityForResult(i, StartActivityForResult.PLUGIN_REQUEST_SETTINGS));
//                                    }
//                                } catch (RemoteException | NullPointerException e) {
//                                    //TODO toast
//                                }
//                            }
//                        });
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    boolean hasAbility(int ability) {
        return (capabilities & ability) != 0;
    }

    static int[] toArray(Collection<Integer> collection) {
        Object[] boxedArray = collection.toArray();
        int len = boxedArray.length;
        int[] array = new int[len];
        for (int i = 0; i < len; i++) {
            array[i] = ((Integer) boxedArray[i]).intValue();
        }
        return array;
    }
}
