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

package org.opensilk.music.ui.nav.adapter;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.opensilk.music.R;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.nav.NavItem;
import org.opensilk.music.ui.nav.NavItem.Type;
import org.opensilk.music.util.MarkedForRemoval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by drew on 7/15/14.
 */
@MarkedForRemoval
public class NavAdapter extends ArrayAdapter<NavItem> {

    public NavAdapter(Context context) {
        super(context, -1, makeDefaultNavList(context));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) convertView;
        NavItem item = getItem(position);
        if (v == null) {
           v = (TextView) LayoutInflater.from(getContext()).inflate(
                   item.type == Type.HEADER ? R.layout.drawer_list_header : R.layout.drawer_list_item,
                   parent, false);
        }
        v.setText(item.title);
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type.ordinal();
    }

    public void addPlugins(Collection<PluginInfo> infos) {
        insert(new NavItem(Type.HEADER, getContext().getString(R.string.drawer_library), null), getCount()-1);
        for (final PluginInfo info : infos) {
            insert(new NavItem(Type.ITEM, info.title, new Runnable() {
                @Override
                public void run() {
                    NavUtils.openLibrary((FragmentActivity) getContext(), info);
                }
            }), getCount()-1);
        }
    }

    public static List<NavItem> makeDefaultNavList(final Context context) {
        List<NavItem> navItems = new ArrayList<>();
        navItems.add(new NavItem(Type.HEADER, context.getString(R.string.drawer_device), null));
        navItems.add(new NavItem(Type.ITEM, context.getString(R.string.my_library), new Runnable() {
            @Override
            public void run() {
                NavUtils.goHome((FragmentActivity) context);
            }
        }));
        navItems.add(new NavItem(Type.ITEM, context.getString(R.string.folders), new Runnable() {
            @Override
            public void run() {
                NavUtils.openFoldersFragment((FragmentActivity) context);
            }
        }));
        navItems.add(new NavItem(Type.HEADER, context.getString(R.string.menu_settings), new Runnable() {
            @Override
            public void run() {
                NavUtils.openSettings((FragmentActivity) context);
            }
        }));
        // XXX when adding new items be sure to update addPlugins();
        return navItems;
    }


}
