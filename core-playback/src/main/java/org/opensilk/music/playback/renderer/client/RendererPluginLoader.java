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

package org.opensilk.music.playback.renderer.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.playback.R;
import org.opensilk.music.playback.renderer.RendererConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by drew on 11/15/14.
 */
public class RendererPluginLoader {

    final Context context;

    @Inject
    public RendererPluginLoader(
            @ForApplication Context context
    ) {
        this.context = context;
    }

    public Observable<List<RendererInfo>> getObservable() {
        return Observable.create(new Observable.OnSubscribe<List<RendererInfo>>() {
            @Override
            public void call(Subscriber<? super List<RendererInfo>> subscriber) {
                try {
                    List<RendererInfo> list = getPluginInfos(true);
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

    public List<RendererInfo> getPluginInfos() {
        return getPluginInfos(false);
    }

    public List<RendererInfo> getPluginInfos(boolean wantIcon) {
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> resolveInfos = pm.queryIntentServices(
                new Intent(RendererConstants.ACTION_RENDERER_SERVICE), PackageManager.GET_META_DATA);
        final List<RendererInfo> pluginInfos = new ArrayList<>(resolveInfos.size()+1);
        for (final ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo == null || resolveInfo.serviceInfo == null)
                continue;
            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            //XXX for now we only allow renderers in our own package
            if (!StringUtils.equals(serviceInfo.packageName, context.getPackageName()))
                continue;
            //ignore renderers we cant access
            final String permission = serviceInfo.permission;
            if (!StringUtils.isEmpty(permission) && context.getPackageManager().checkPermission(
                    permission, context.getPackageName()) != PackageManager.PERMISSION_GRANTED)
                continue;
            final RendererInfo pi = readResolveInfo(pm, resolveInfo);
            if (!wantIcon)
                pi.setIcon(null);
            pluginInfos.add(pi);
        }
        Collections.sort(pluginInfos);
        //add local renderer as first
        pluginInfos.add(0, makeDefaultRendererInfo());
        return pluginInfos;
    }

    private RendererInfo makeDefaultRendererInfo() {
        String title = context.getString(R.string.renderer_default);
        String desc = context.getString(R.string.renderer_default_description);
        return new RendererInfo(title, desc, null);
    }

    private RendererInfo readResolveInfo(PackageManager pm, ResolveInfo resolveInfo) {
        final CharSequence title = resolveInfo.loadLabel(pm);
        final ComponentName cn = getComponentName(resolveInfo);
        CharSequence description;
        try {
            Context packageContext = context.createPackageContext(cn.getPackageName(), 0);
            Resources packageRes = packageContext.getResources();
            description = packageRes.getString(resolveInfo.serviceInfo.descriptionRes);
        } catch (PackageManager.NameNotFoundException e) {
            description = null;
        }
        final String titleS = title != null ? title.toString() : "";
        final String descS = description != null ? description.toString() : "";
        final RendererInfo pluginInfo = new RendererInfo(titleS, descS, cn);
        final Drawable icon = resolveInfo.loadIcon(pm);
        pluginInfo.setIcon(icon);
        final ComponentName activityCn = getActivityComponentName(resolveInfo);
        pluginInfo.setActivityComponent(activityCn);
        return pluginInfo;
    }

    private static ComponentName getComponentName(ResolveInfo resolveInfo) {
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private static ComponentName getActivityComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo.serviceInfo.metaData != null) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            Bundle meta = serviceInfo.metaData;
            if (meta.getString(RendererConstants.META_PICKER_ACTIVITY) != null) {
                return new ComponentName(serviceInfo.packageName,
                        meta.getString(RendererConstants.META_PICKER_ACTIVITY));
            }
        }
        return null;
    }

}
