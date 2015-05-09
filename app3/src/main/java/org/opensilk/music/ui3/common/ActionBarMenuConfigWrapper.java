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

package org.opensilk.music.ui3.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryConstants;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.MusicActivityComponent;

import javax.inject.Inject;

import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class ActionBarMenuConfigWrapper {

    final LibraryConfig libraryConfig;
    final LibraryInfo libraryInfo;

    @Inject
    public ActionBarMenuConfigWrapper(LibraryConfig libraryConfig, LibraryInfo libraryInfo) {
        this.libraryConfig = libraryConfig;
        this.libraryInfo = libraryInfo;
    }

    public ActionBarMenuConfig injectCommonItems(ActionBarMenuConfig originalConfig) {
        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        if (originalConfig.menus != null && originalConfig.menus.length > 0) {
            builder.withMenus(originalConfig.menus);
        }
        if (originalConfig.customMenus != null && originalConfig.customMenus.length > 0) {
            builder.withMenus(originalConfig.customMenus);
        }

        // device selection
        String selectName = libraryConfig.getMeta(LibraryConfig.META_MENU_NAME_PICKER);
        if (!TextUtils.isEmpty(selectName)) {
            builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
                    0, R.id.menu_change_source, 99, selectName, -1));
        } else {
            builder.withMenu(R.menu.library_change_source);
        }

        // library settings
        if (libraryConfig.hasAbility(LibraryCapability.SETTINGS)) {
            String settingsName = libraryConfig.getMeta(LibraryConfig.META_MENU_NAME_SETTINGS);
            if (!TextUtils.isEmpty(settingsName)) {
                builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
                        0, R.id.menu_library_settings, 100, settingsName, -1));
            } else {
                builder.withMenu(R.menu.library_settings);
            }
        }

        DelegateHandler handler = new DelegateHandler(originalConfig.actionHandler) {
            @Override
            public Boolean call(Context context, Integer integer) {
                MusicActivityComponent component = DaggerService.getDaggerComponent(context);
                AppPreferences appPreferences = component.appPreferences();
                ActivityResultsController activityResultsController = component.activityResultsController();
                boolean handled = super.call(context, integer);
                if (!handled) {
                    switch (integer) {
                        case R.id.menu_change_source:
                            appPreferences.removeDefaultLibraryInfo(libraryConfig);
                            //TODO
                            handled = true;
                            break;
                        case R.id.menu_library_settings:
                            Intent intent = new Intent()
                                    .setComponent(libraryConfig.<ComponentName>getMeta(LibraryConfig.META_SETTINGS_COMPONENT))
                                    .putExtra(LibraryConstants.EXTRA_LIBRARY_ID, libraryInfo.libraryId)
                                    .putExtra(LibraryConstants.EXTRA_LIBRARY_INFO, libraryInfo);
                            activityResultsController.startActivityForResult(intent, ActivityRequestCodes.LIBRARY_SETTINGS, null);
                            handled = true;
                            break;
                    }
                }
                return handled;
            }
        };

        return builder.setActionHandler(handler).build();
    }

    public abstract static class DelegateHandler implements Func2<Context, Integer, Boolean> {
        final Func2<Context, Integer, Boolean> delegate;

        public DelegateHandler(Func2<Context, Integer, Boolean> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Boolean call(Context context, Integer integer) {
            if (delegate != null) {
                return delegate.call(context, integer);
            }
            return false;
        }
    }
}
