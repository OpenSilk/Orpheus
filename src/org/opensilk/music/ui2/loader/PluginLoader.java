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

package org.opensilk.music.ui2.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonWriter;

import org.apache.commons.io.IOUtils;
import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.plugin.folders.FolderLibraryService;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

/**
 * Created by drew on 11/15/14.
 */
@Singleton
public class PluginLoader {

    public static final String PREF_DISABLED_PLUGINS = "disabled_plugins";
    public static final String API_PLUGIN_SERVICE = OrpheusApi.ACTION_LIBRARY_SERVICE;

    final Context context;
    final AppPreferences settings;

    @Inject
    public PluginLoader(@ForApplication Context context,
                        AppPreferences settings) {
        this.context = context;
        this.settings = settings;
    }

    public Observable<List<PluginInfo>> getObservable() {
        return Observable.create(new Observable.OnSubscribe<List<PluginInfo>>() {
            @Override
            public void call(Subscriber<? super List<PluginInfo>> subscriber) {
                try {
                    List<PluginInfo> list = getActivePlugins(true);
                    if (list == null) {
                        list = Collections.emptyList();
                    }
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onNext(list);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    if (subscriber.isUnsubscribed()) return;
                    subscriber.onError(e);
                }
            }
        });
    }

    public List<PluginInfo> getPluginInfos() {
        return getPluginInfos(false);
    }

    public List<PluginInfo> getPluginInfos(boolean wantIcon) {
        final List<ComponentName> disabledPlugins = readDisabledPlugins();
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(
                new Intent(API_PLUGIN_SERVICE), PackageManager.GET_META_DATA
        );
        final List<PluginInfo> pluginInfos = new ArrayList<PluginInfo>(resolveInfos.size()+1);
        for (final ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null)
                continue;
            final PluginInfo pi = readResolveInfo(pm, disabledPlugins, resolveInfo);
            if (!wantIcon)
                pi.icon = null;
            pluginInfos.add(pi);
        }
        Collections.sort(pluginInfos);
        // folders is special and is always first
        List<ResolveInfo> folderResolveInfos = pm.queryIntentServices(
                new Intent(context, FolderLibraryService.class), PackageManager.GET_META_DATA
        );
        if (folderResolveInfos != null && folderResolveInfos.size() > 0) {
            final ResolveInfo resolveInfo = folderResolveInfos.get(0);
            if (resolveInfo.serviceInfo != null) {
                final PluginInfo pi = readResolveInfo(pm, disabledPlugins, resolveInfo);
                if (!wantIcon)
                    pi.icon = null;
                pluginInfos.add(0, pi);
            }
        }
        return pluginInfos;
    }

    public List<PluginInfo> getActivePlugins() {
        return getActivePlugins(false);
    }

    public List<PluginInfo> getActivePlugins(boolean wantIcon) {
        final List<PluginInfo> plugins = getPluginInfos(wantIcon);
        if (plugins != null && !plugins.isEmpty()) {
            final Iterator<PluginInfo> ii = plugins.iterator();
            while (ii.hasNext()) {
                final PluginInfo p = ii.next();
                if (!p.isActive) {
                    ii.remove();
                }
            }
        }
        return plugins;
    }

    private PluginInfo readResolveInfo(PackageManager pm, List<ComponentName> disabledPlugins, ResolveInfo resolveInfo) {
        boolean hasPermission = false;
        final String permission = resolveInfo.serviceInfo.permission;
        if (TextUtils.equals(permission, OrpheusApi.PERMISSION_BIND_LIBRARY_SERVICE)
                || TextUtils.equals(permission, "org.opensilk.music.debug.api.permission.BIND_LIBRARY_SERVICE")) {
            hasPermission = true;
        }
        final CharSequence title = resolveInfo.loadLabel(pm);
        final ComponentName cn = getComponentName(resolveInfo);
        final Drawable icon = resolveInfo.loadIcon(pm);
        CharSequence description;
        try {
            Context packageContext = context.createPackageContext(cn.getPackageName(), 0);
            Resources packageRes = packageContext.getResources();
            description = packageRes.getString(resolveInfo.serviceInfo.descriptionRes);
        } catch (PackageManager.NameNotFoundException e) {
            description = null;
        }
        PluginInfo pluginInfo = new PluginInfo(title, description, cn);
        pluginInfo.hasPermission = hasPermission;
        pluginInfo.icon = icon;
        for (ComponentName c : disabledPlugins) {
            if (c.equals(pluginInfo.componentName)) {
                pluginInfo.isActive = false;
                break;
            }
        }
        return pluginInfo;
    }

    private static ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null)
            return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    public void setPluginEnabled(ComponentName plugin) {
        List<ComponentName> disabledPlugins = readDisabledPlugins();
        Iterator<ComponentName> ii = disabledPlugins.iterator();
        while (ii.hasNext()) {
            if (plugin.equals(ii.next())) {
                ii.remove();
            }
        }
        writeDisabledPlugins(disabledPlugins);
    }

    public void setPluginDisabled(ComponentName plugin) {
        List<ComponentName> disabledPlugins = readDisabledPlugins();
        for (ComponentName cn : disabledPlugins) {
            if (plugin.equals(cn)) {
                return;
            }
        }
        disabledPlugins.add(plugin);
        writeDisabledPlugins(disabledPlugins);
    }

    public List<ComponentName> readDisabledPlugins() {
        List<ComponentName> list = new ArrayList<>();
        String json = settings.getString(PREF_DISABLED_PLUGINS, null);
        Timber.v("Read disabled plugins=" + json);
        if (json != null) {
            JsonReader jr = new JsonReader(new StringReader(json));
            try {
                jr.beginArray();
                while(jr.hasNext()) {
                    list.add(ComponentName.unflattenFromString(jr.nextString()));
                }
                jr.endArray();
            } catch (IOException e) {
                settings.remove(PREF_DISABLED_PLUGINS);
                list.clear();
            } finally {
                IOUtils.closeQuietly(jr);
            }
        }
        return list;
    }

    public void writeDisabledPlugins(List<ComponentName> plugins) {
        StringWriter sw = new StringWriter(100);
        JsonWriter jw = new JsonWriter(sw);
        try {
            jw.beginArray();
            for (ComponentName cn : plugins) {
                jw.value(cn.flattenToString());
            }
            jw.endArray();
            Timber.v("Write disabled plugins=" + sw.toString());
            settings.putString(PREF_DISABLED_PLUGINS, sw.toString());
        } catch (IOException e) {
            settings.remove(PREF_DISABLED_PLUGINS);
        } finally {
            IOUtils.closeQuietly(jw);
        }
    }
}
