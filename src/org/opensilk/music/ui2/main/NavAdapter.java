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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.settings.SettingsActivity;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.library.PluginScreen;

import java.util.Collection;

/**
 * Created by drew on 4/20/15.
 */
public class NavAdapter extends ArrayAdapter<NavItem> {
    final boolean lightTheme;

    public NavAdapter(Context context, Collection<PluginInfo> plugins) {
        super(context, -1);
        lightTheme = ThemeUtils.isLightTheme(context);
        loadPlugins(plugins);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) convertView;
        NavItem item = getItem(position);
        if (v == null) {
            v = (TextView) LayoutInflater.from(getContext()).inflate(
                    item.type == NavItem.Type.HEADER ? R.layout.drawer_list_header : R.layout.drawer_list_item,
                    parent, false);
        }
        if (!TextUtils.isEmpty(item.title)) {
            v.setText(item.title);
        } else {
            v.setText(item.titleRes);
        }
        if (item.icon != null) {
            v.setCompoundDrawables(item.icon, null, null, null);
        } else if (item.iconRes >= 0) {
            v.setCompoundDrawablesWithIntrinsicBounds(item.iconRes, 0, 0, 0);
        }
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return NavItem.Type.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type.ordinal();
    }

    public void loadPlugins(Collection<PluginInfo> infos) {
        add(NavItem.Factory.newItem(R.string.my_library, R.drawable.ic_my_library_music_grey600_24dp, new GalleryScreen()));
        for (final PluginInfo info : infos) {
            Drawable d = info.icon;
            info.icon = null; //free plugin reference
            if (d == null) {
                d = getContext().getResources().getDrawable(R.drawable.ic_extension_grey600_24dp);
            }
            int bounds = (int) (24 * getContext().getResources().getDisplayMetrics().density);
            d.setBounds(0, 0, bounds, bounds);
            add(NavItem.Factory.newItem(info.title, d, new PluginScreen(info)));
        }
        add(NavItem.Factory.newHeader(R.string.menu_settings, R.drawable.ic_settings_grey600_24dp,
                new StartActivityForResult(new Intent(getContext(), SettingsActivity.class),
                        StartActivityForResult.APP_REQUEST_SETTINGS)));
    }

}
