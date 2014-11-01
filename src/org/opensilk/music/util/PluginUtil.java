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

package org.opensilk.music.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonWriter;

import org.apache.commons.io.IOUtils;
import org.opensilk.music.R;

import org.opensilk.music.api.OrpheusApi;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.home.HomeFragment;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 6/8/14.
 */
public class PluginUtil {

    public static final String PREF_DISABLED_PLUGINS = "disabled_plugins";
    public static final String API_PLUGIN_SERVICE = OrpheusApi.ACTION_LIBRARY_SERVICE;

    public static List<PluginInfo> getPluginInfos(Context context) {
        return getPluginInfos(context, false);
    }

    public static List<PluginInfo> getPluginInfos(Context context, boolean wantIcon) {
        List<ComponentName> disabledPlugins = PluginUtil.readDisabledPlugins(context);
        PackageManager pm = context.getPackageManager();
        Intent dreamIntent = new Intent(API_PLUGIN_SERVICE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(dreamIntent, PackageManager.GET_META_DATA);
        List<PluginInfo> pluginInfos = new ArrayList<PluginInfo>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null)
                continue;
            CharSequence title = resolveInfo.loadLabel(pm);
            ComponentName cn = getComponentName(resolveInfo);
            Drawable icon = resolveInfo.loadIcon(pm);
            CharSequence description;
            try {
                Context packageContext = context.createPackageContext(cn.getPackageName(), 0);
                Resources packageRes = packageContext.getResources();
                description = packageRes.getString(resolveInfo.serviceInfo.descriptionRes);
            } catch (PackageManager.NameNotFoundException e) {
                description = null;
            }
            PluginInfo pluginInfo = new PluginInfo(title, description, cn);
            //if (wantIcon) pluginInfo.icon = icon;
            for (ComponentName c : disabledPlugins) {
                if (c.equals(pluginInfo.componentName)) {
                    pluginInfo.isActive = false;
                    break;
                }
            }
            pluginInfos.add(pluginInfo);
        }
        return pluginInfos;
    }

    public static List<PluginInfo> getActivePlugins(Context context) {
        return getActivePlugins(context, false);
    }

    public static List<PluginInfo> getActivePlugins(Context context, boolean wantIcon) {
        List<PluginInfo> plugins = getPluginInfos(context, wantIcon);
        List<ComponentName> disabledComponets = readDisabledPlugins(context);
        if (plugins != null && !plugins.isEmpty()) {
            if (disabledComponets != null && !disabledComponets.isEmpty()) {
                Iterator<PluginInfo> ii = plugins.iterator();
                while (ii.hasNext()) {
                    PluginInfo p = ii.next();
                    for (ComponentName c : disabledComponets) {
                        if (c.equals(p.componentName)) {
                            ii.remove();
                            break;
                        }
                    }
                }
            }
        }
        return plugins;
    }

    private static ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null)
            return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    public static void setPluginEnabled(Context context, ComponentName plugin) {
        List<ComponentName> disabledPlugins = readDisabledPlugins(context);
        Iterator<ComponentName> ii = disabledPlugins.iterator();
        while (ii.hasNext()) {
            if (plugin.equals(ii.next())) {
                ii.remove();
            }
        }
        writeDisabledPlugins(context, disabledPlugins);
    }

    public static void setPluginDisabled(Context context, ComponentName plugin) {
        List<ComponentName> disabledPlugins = readDisabledPlugins(context);
        for (ComponentName cn : disabledPlugins) {
            if (plugin.equals(cn)) {
                return;
            }
        }
        disabledPlugins.add(plugin);
        writeDisabledPlugins(context, disabledPlugins);
    }

    public static List<ComponentName> readDisabledPlugins(Context context) {
        List<ComponentName> list = new ArrayList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sp.getString(PREF_DISABLED_PLUGINS, null);
        Timber.d("Read disabled plugins=" + json);
        if (json != null) {
            JsonReader jr = new JsonReader(new StringReader(json));
            try {
                jr.beginArray();
                while(jr.hasNext()) {
                    list.add(ComponentName.unflattenFromString(jr.nextString()));
                }
                jr.endArray();
            } catch (IOException e) {
                sp.edit().remove(PREF_DISABLED_PLUGINS).apply();
                list.clear();
            } finally {
                IOUtils.closeQuietly(jr);
            }
        }
        return list;
    }

    public static void writeDisabledPlugins(Context context, List<ComponentName> plugins) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        StringWriter sw = new StringWriter(100);
        JsonWriter jw = new JsonWriter(sw);
        try {
            jw.beginArray();
            for (ComponentName cn : plugins) {
                jw.value(cn.flattenToString());
            }
            jw.endArray();
            Timber.d("Write disabled plugins="+sw.toString());
            sp.edit().putString(PREF_DISABLED_PLUGINS, sw.toString()).apply();
        } catch (IOException e) {
            sp.edit().remove(PREF_DISABLED_PLUGINS).apply();
        } finally {
            try {
                jw.close();
            } catch (IOException ignored) {}
        }
    }

}
