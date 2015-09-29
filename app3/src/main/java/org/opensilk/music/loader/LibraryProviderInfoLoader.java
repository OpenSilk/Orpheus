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

package org.opensilk.music.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.library.provider.LibraryProviderOld;
import org.opensilk.music.library.provider.LibraryUris;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

import static org.opensilk.music.library.provider.LibraryMethods.CONFIG;

/**
 * Created by drew on 11/15/14.
 */
public class LibraryProviderInfoLoader {

    final Context context;
    final AppPreferences settings;

    @Inject
    public LibraryProviderInfoLoader(
            @ForApplication Context context,
            AppPreferences settings
    ) {
        this.context = context;
        this.settings = settings;
    }

    public Observable<List<LibraryProviderInfo>> getActivePlugins() {
        return makeObservableApi19()
                .filter(new Func1<LibraryProviderInfo, Boolean>() {
                    @Override
                    public Boolean call(LibraryProviderInfo libraryProviderInfo) {
                        return libraryProviderInfo.isActive;
                    }
                })
                .toSortedList(new Func2<LibraryProviderInfo, LibraryProviderInfo, Integer>() {
                    @Override
                    public Integer call(LibraryProviderInfo libraryProviderInfo, LibraryProviderInfo libraryProviderInfo2) {
                        return libraryProviderInfo.compareTo(libraryProviderInfo2);
                    }
                });
    }

    public Observable<List<LibraryProviderInfo>> getPlugins() {
        return makeObservableApi19()
                .toSortedList(new Func2<LibraryProviderInfo, LibraryProviderInfo, Integer>() {
                    @Override
                    public Integer call(LibraryProviderInfo libraryProviderInfo, LibraryProviderInfo libraryProviderInfo2) {
                        return libraryProviderInfo.compareTo(libraryProviderInfo2);
                    }
                });
    }

    public Observable<LibraryProviderInfo> makeObservable() {
        final List<String> disabledPlugins = settings.readDisabledPlugins();
        return Observable.create(new Observable.OnSubscribe<List<ProviderInfo>>() {
            @Override
            public void call(Subscriber<? super List<ProviderInfo>> subscriber) {
                final PackageManager pm = context.getPackageManager();
                final List<ProviderInfo> providerInfos = pm.queryContentProviders(null, 0, PackageManager.GET_META_DATA);
                subscriber.onNext(providerInfos);
                subscriber.onCompleted();
            }
        }).flatMap(new Func1<List<ProviderInfo>, Observable<ProviderInfo>>() {
            @Override
            public Observable<ProviderInfo> call(List<ProviderInfo> providerInfos) {
                return Observable.from(providerInfos);
            }
        }).filter(new Func1<ProviderInfo, Boolean>() {
            @Override
            public Boolean call(ProviderInfo providerInfo) {
                return StringUtils.startsWith(providerInfo.authority, LibraryProviderOld.AUTHORITY_PFX)
                        //Ignore non exported providers unless they're ours
                        && (StringUtils.equals(providerInfo.packageName, context.getPackageName()) || providerInfo.exported);
            }
        }).map(new Func1<ProviderInfo, LibraryProviderInfo>() {
            @Override
            public LibraryProviderInfo call(ProviderInfo providerInfo) {
                final PackageManager pm = context.getPackageManager();
                final String authority = providerInfo.authority;
                final CharSequence title = providerInfo.loadLabel(pm);
                final ComponentName cn = new ComponentName(providerInfo.packageName, providerInfo.name);
                final Drawable icon = providerInfo.loadIcon(pm);
                CharSequence description;
                try {
                    Context packageContext = context.createPackageContext(cn.getPackageName(), 0);
                    Resources packageRes = packageContext.getResources();
                    description = packageRes.getString(providerInfo.descriptionRes);
                } catch (PackageManager.NameNotFoundException e) {
                    description = "";
                }
                final LibraryProviderInfo lpi = new LibraryProviderInfo(title.toString(), description.toString(), authority);
                lpi.icon = icon;
                for (String a : disabledPlugins) {
                    if (a.equals(lpi.authority)) {
                        lpi.isActive = false;
                        break;
                    }
                }
                return lpi;
            }
        });
    }

    public Observable<LibraryProviderInfo> makeObservableApi19() {
        final List<String> disabledPlugins = settings.readDisabledPlugins();
        return Observable.create(new Observable.OnSubscribe<List<ResolveInfo>>() {
            @Override
            public void call(Subscriber<? super List<ResolveInfo>> subscriber) {
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> resolveInfos = pm.queryIntentContentProviders(
                        new Intent().setAction(LibraryProvider.ACTION_FILTER), PackageManager.GET_META_DATA);
                subscriber.onNext(resolveInfos);
                subscriber.onCompleted();
            }
        }).flatMap(new Func1<List<ResolveInfo>, Observable<ResolveInfo>>() {
            @Override
            public Observable<ResolveInfo> call(List<ResolveInfo> providerInfos) {
                return Observable.from(providerInfos);
            }
        }).filter(new Func1<ResolveInfo, Boolean>() {
            @Override
            public Boolean call(ResolveInfo resolveInfo) {
                ProviderInfo providerInfo = resolveInfo.providerInfo;
                return providerInfo != null
                        //Ignore non exported providers unless they're ours
                        && (StringUtils.equals(providerInfo.packageName, context.getPackageName()) || providerInfo.exported);
            }
        }).map(new Func1<ResolveInfo, LibraryProviderInfo>() {
            @Override
            public LibraryProviderInfo call(ResolveInfo resolveInfo) {
                ProviderInfo providerInfo = resolveInfo.providerInfo;
                final PackageManager pm = context.getPackageManager();
                final String authority = providerInfo.authority;
                final CharSequence title = providerInfo.loadLabel(pm);
                final ComponentName cn = new ComponentName(providerInfo.packageName, providerInfo.name);
                final Drawable icon = providerInfo.loadIcon(pm);
                CharSequence description;
                try {
                    Context packageContext = context.createPackageContext(cn.getPackageName(), 0);
                    Resources packageRes = packageContext.getResources();
                    description = packageRes.getString(providerInfo.descriptionRes);
                } catch (PackageManager.NameNotFoundException e) {
                    description = "";
                }
                final LibraryProviderInfo lpi = new LibraryProviderInfo(title.toString(), description.toString(), authority);
                lpi.icon = icon;
                for (String a : disabledPlugins) {
                    if (a.equals(lpi.authority)) {
                        lpi.isActive = false;
                        break;
                    }
                }
                return lpi;
            }
        });
    }

}
