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

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class NavView extends ListView {

    @Inject
    NavViewBlueprint.Presenter presenter;

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
                } else if (item.intent != null) {
                    presenter.open(item.intent);
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
        public final CharSequence title;
        public final Screen screen;
        public final StartActivityForResult intent;

        public Item(Type type, CharSequence title, Screen screen, StartActivityForResult intent) {
            this.type = type;
            this.title = title;
            this.screen = screen;
            this.intent = intent;
        }

    }

    public static class Adapter extends ArrayAdapter<Item> {

        public Adapter(Context context, Collection<PluginInfo> plugins) {
            super(context, -1);
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
            v.setText(item.title);
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
            add(new Item(Item.Type.ITEM, getContext().getString(R.string.my_library), new GalleryScreen(), null));
            add(new Item(Item.Type.ITEM, getContext().getString(R.string.folders), new FolderScreen(), null));
            for (final PluginInfo info : infos) {
                add(new Item(Item.Type.ITEM, info.title, new PluginScreen(info), null));
            }
            add(new Item(Item.Type.HEADER, getContext().getString(R.string.menu_settings), null,
                    new StartActivityForResult(new Intent(getContext(), SettingsActivity.class),
                            StartActivityForResult.APP_REQUEST_SETTINGS)));
        }

    }

}
