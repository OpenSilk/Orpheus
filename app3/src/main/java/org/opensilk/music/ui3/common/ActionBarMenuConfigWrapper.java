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

import android.content.Context;
import android.text.TextUtils;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryCapability;
import org.opensilk.music.library.LibraryConfig;

import javax.inject.Inject;

import rx.functions.Func2;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
@Deprecated
public class ActionBarMenuConfigWrapper {

    final LibraryConfig libraryConfig;
    final FragmentManagerOwner fm;
    final AppPreferences settings;
    final ActivityResultsController activityResultsController;

    @Inject
    public ActionBarMenuConfigWrapper(
            LibraryConfig libraryConfig,
            FragmentManagerOwner fm,
            AppPreferences settings,
            ActivityResultsController activityResultsController
    ) {
        this.libraryConfig = libraryConfig;
        this.fm = fm;
        this.settings = settings;
        this.activityResultsController = activityResultsController;
    }

    public ActionBarMenuConfig injectCommonItems(ActionBarMenuConfig originalConfig) {
        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        if (originalConfig != null && originalConfig.getMenus() != null && originalConfig.getMenus().length > 0) {
            builder.withMenus(originalConfig.getCustomMenus());
        }
        if (originalConfig != null && originalConfig.getCustomMenus() != null && originalConfig.getCustomMenus().length > 0) {
            builder.withMenus(originalConfig.getCustomMenus());
        }

        applyCommonItems(builder);

        Func2<Context, Integer, Boolean> wrappedHandler =
                originalConfig != null ? originalConfig.getActionHandler() : null;

        return builder.setActionHandler(getDelegateHandler(wrappedHandler)).build();
    }

    public void applyCommonItems(ActionBarMenuConfig.Builder builder) {
//        // device selection
//        if (!libraryConfig.getMeta().getBoolean(LibraryConfig.META_MENU_PICKER_HIDE, false)) {
//            String selectName = libraryConfig.getMeta(LibraryConfig.META_MENU_PICKER_NAME);
//            if (!TextUtils.isEmpty(selectName)) {
//                builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
//                        0, R.id.menu_change_source, 99, selectName, -1));
//            } else {
//                builder.withMenu(R.menu.library_change_source);
//            }
//        }

        // library settings
        if (libraryConfig.hasFlag(LibraryCapability.SETTINGS)) {
            String settingsName = libraryConfig.getMeta(LibraryConfig.META_MENU_NAME_SETTINGS);
            if (!TextUtils.isEmpty(settingsName)) {
                builder.withMenu(new ActionBarMenuConfig.CustomMenuItem(
                        0, R.id.menu_library_settings, 100, settingsName, -1));
            } else {
                builder.withMenu(R.menu.library_settings);
            }
        }
    }

    public DelegateHandler getDelegateHandler(Func2<Context, Integer, Boolean> wrappedHandler) {
        return new DelegateHandler(wrappedHandler) {
            @Override
            public Boolean call(Context context, Integer integer) {
                boolean handled = super.call(context, integer);
                if (!handled) {
//                    switch (integer) {
//                        case R.id.menu_change_source:
//                            settings.removeLibraryInfo(libraryConfig, AppPreferences.DEFAULT_LIBRARY);
//                            fm.killBackStack();
//                            fm.replaceMainContent(LandingScreenFragment.ni(libraryConfig), false);
//                            handled = true;
//                            break;
//                        case R.id.menu_library_settings:
//                            Intent intent = new Intent().setComponent(libraryConfig.<ComponentName>getMeta(LibraryConfig.META_SETTINGS_COMPONENT));
//                            activityResultsController.startActivityForResult(intent, ActivityRequestCodes.LIBRARY_SETTINGS, null);
//                            handled = true;
//                            break;
//                    }
                }
                return handled;
            }
        };
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
