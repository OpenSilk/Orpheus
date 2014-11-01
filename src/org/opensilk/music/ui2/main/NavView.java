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
import org.opensilk.music.R;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui.settings.SettingsActivity;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.ui2.folder.FolderScreen;
import org.opensilk.music.ui2.gallery.GalleryScreen;
import org.opensilk.music.ui2.library.PluginScreen;
import org.opensilk.music.ui2.theme.Themer;

import java.util.Collection;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class NavView extends ListView {

    @Inject
    NavBlueprint.Presenter presenter;

    public NavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void onLoad(Collection<PluginInfo> infos) {
        setAdapter(new Adapter(getContext(), infos));
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = getAdapter().getItem(position);
                if (item.screen != null) {
                    setItemChecked(position, true);
                    presenter.go(getContext(), item.screen);
                } else if (item.event != null) {
                    presenter.open(item.event);
                }
            }
        });
    }

    @Override
    public Adapter getAdapter() {
        return (Adapter) super.getAdapter();
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

        public Item(Type type, CharSequence title, Drawable icon, Screen screen) {
            this.type = type;
            this.titleRes = -1;
            this.title = title;
            this.icon = icon;
            this.iconRes = -1;
            this.screen = screen;
            this.event = null;
        }

        public Item(Type type, int titleRes, int iconRes, Screen screen) {
            this.type = type;
            this.titleRes = titleRes;
            this.title = null;
            this.iconRes = iconRes;
            this.icon = null;
            this.screen = screen;
            this.event = null;
        }

        public Item(Type type, int titleRes, int iconRes, Object event) {
            this.type = type;
            this.titleRes = titleRes;
            this.title = null;
            this.iconRes = iconRes;
            this.icon = null;
            this.screen = null;
            this.event = event;
        }

    }

    public static class Adapter extends ArrayAdapter<Item> {
        final boolean lightTheme;

        public Adapter(Context context, Collection<PluginInfo> plugins) {
            super(context, -1);
            lightTheme = Themer.isLightTheme(context);
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
            add(new Item(Item.Type.ITEM, R.string.my_library, R.drawable.ic_my_library_music_grey600_24dp, new GalleryScreen()));
            add(new Item(Item.Type.ITEM, R.string.folders, R.drawable.ic_folder_grey600_24dp, new FolderScreen()));
            for (final PluginInfo info : infos) {
                Drawable d = null;// info.icon;
                //info.icon = null;
                if (d == null) {
                    d = getContext().getResources().getDrawable(R.drawable.ic_extension_grey600_24dp);
                }
                int bounds = (int) (24 * getContext().getResources().getDisplayMetrics().density);
                d.setBounds(0,0, bounds, bounds);
                add(new Item(Item.Type.ITEM, info.title, d, new PluginScreen(info)));
            }
            add(new Item(Item.Type.HEADER, R.string.menu_settings, R.drawable.ic_settings_grey600_24dp,
                    new StartActivityForResult(new Intent(getContext(), SettingsActivity.class),
                            StartActivityForResult.APP_REQUEST_SETTINGS)));
        }

    }

}
