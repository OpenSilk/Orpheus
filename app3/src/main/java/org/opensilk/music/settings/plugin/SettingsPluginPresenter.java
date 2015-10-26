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

package org.opensilk.music.settings.plugin;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.library.client.LibraryProviderInfoLoader;

import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by drew on 5/24/15.
 */
@ScreenScope
public class SettingsPluginPresenter extends ViewPresenter<SettingsPluginRecyclerView>
        implements ItemClickSupport.OnItemClickListener {

    final LibraryProviderInfoLoader loader;
    final AppPreferences settings;

    @Inject
    public SettingsPluginPresenter(LibraryProviderInfoLoader loader, AppPreferences settings) {
        this.loader = loader;
        this.settings = settings;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        loader.getPlugins().subscribe(new Action1<List<LibraryProviderInfo>>() {
            @Override
            public void call(List<LibraryProviderInfo> libraryProviderInfos) {
                if (hasView()) {
                    getView().getAdapter().replaceAll(libraryProviderInfos);
                    getView().showList(false);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                //TODO
            }
        }, new Action0() {
            @Override
            public void call() {
                if (hasView() && getView().getAdapter().isEmpty()) {
                    getView().showEmpty(false);
                    //TODO
                }
            }
        });
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        if (!hasView()) return;
        LibraryProviderInfo item = getView().getAdapter().getItem(position);
        if (item.isActive) {
            item.isActive = false;
            settings.setPluginDisabled(item.authority);
        } else {
            item.isActive = true;
            settings.setPluginEnabled(item.authority);
        }
        getView().getAdapter().notifyItemChanged(position);
    }

}
