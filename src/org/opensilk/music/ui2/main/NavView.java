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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.settings.SettingsActivity;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.library.PluginScreen;

import java.util.Collection;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class NavView extends ListView {

    @Inject Nav.Presenter presenter;

    public NavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setDividerHeight(0);
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void onLoad(Collection<PluginInfo> infos) {
        final Adapter adapter = new Adapter(getContext(), infos);
        setAdapter(adapter);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = adapter.getItem(position);
                if (item.screen != null) {
                    setItemChecked(position, true);
                    presenter.go(getContext(), item.screen);
                } else if (item.event != null) {
                    presenter.open(item.event);
                }
            }
        });
    }

    public static class Item {
        public enum Type {
            HEADER,
            ITEM,
        }
        public final Type type;
        public final int titleRes;
        public final CharSequence title;
        public final int iconRes;
        public final Drawable icon;
        public final Screen screen;
        public final Object event;

        public Item(Type type, int titleRes, CharSequence title,
                    int iconRes, Drawable icon, Screen screen, Object event) {
            this.type = type;
            this.titleRes = titleRes;
            this.title = title;
            this.iconRes = iconRes;
            this.icon = icon;
            this.screen = screen;
            this.event = event;
        }

        static class Builder {
            Type type = null;
            int titleRes = -1;
            CharSequence title = null;
            int iconRes = -1;
            Drawable icon = null;
            Screen screen = null;
            Object event = null;

            Item build() {
                return new Item(type, titleRes, title, iconRes, icon, screen, event);
            }
        }

        public static class Factory {

            public static Item newHeader(int titleRes, int iconRes, Object event) {
                Builder b = new Builder();
                b.type = Type.HEADER;
                b.titleRes = titleRes;
                b.iconRes = iconRes;
                b.event = event;
                return b.build();
            }

            public static Item newItem(CharSequence title, Drawable icon, Screen screen) {
                Builder b = new Builder();
                b.type = Type.ITEM;
                b.title = title;
                b.icon = icon;
                b.screen = screen;
                return b.build();
            }

            public static Item newItem(int titleRes, int iconRes, Screen screen) {
                Builder b = new Builder();
                b.type = Type.ITEM;
                b.titleRes = titleRes;
                b.iconRes = iconRes;
                b.screen = screen;
                return b.build();
            }
        }

    }

    public static class Adapter extends ArrayAdapter<Item> {
        final boolean lightTheme;

        public Adapter(Context context, Collection<PluginInfo> plugins) {
            super(context, -1);
            lightTheme = ThemeUtils.isLightTheme(context);
            loadPlugins(plugins);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) convertView;
            Item item = getItem(position);
            if (v == null) {
                v = (TextView) LayoutInflater.from(getContext()).inflate(
                        item.type == Item.Type.HEADER ? R.layout.drawer_list_header : R.layout.drawer_list_item,
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
            return Item.Type.values().length;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).type.ordinal();
        }

        public void loadPlugins(Collection<PluginInfo> infos) {
            add(Item.Factory.newItem(R.string.my_library, R.drawable.ic_my_library_music_grey600_24dp, new GalleryScreen()));
            for (final PluginInfo info : infos) {
                Drawable d = info.icon;
                info.icon = null; //free plugin reference
                if (d == null) {
                    d = getContext().getResources().getDrawable(R.drawable.ic_extension_grey600_24dp);
                }
                int bounds = (int) (24 * getContext().getResources().getDisplayMetrics().density);
                d.setBounds(0,0, bounds, bounds);
                add(Item.Factory.newItem(info.title, d, new PluginScreen(info)));
            }
            add(Item.Factory.newHeader(R.string.menu_settings, R.drawable.ic_settings_grey600_24dp,
                    new StartActivityForResult(new Intent(getContext(), SettingsActivity.class),
                            StartActivityForResult.APP_REQUEST_SETTINGS)));
        }

    }

}
