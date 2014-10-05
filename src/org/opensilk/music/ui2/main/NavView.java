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
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.api.meta.PluginInfo;
import org.opensilk.music.ui2.gallery.GalleryScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import mortar.Blueprint;
import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class NavView extends ListView {

    @Inject
    NavScreen.Presenter presenter;

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

    public void setup() {
        Adapter adapter = new Adapter(getContext());
        setAdapter(adapter);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getCount() == position + 1) {
                    presenter.openSettings(getContext());
                } else {
                    presenter.go(((Item) parent.getItemAtPosition(position)).screen);
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
        public final Blueprint screen;

        public Item(Type type, CharSequence title, Blueprint screen) {
            this.type = type;
            this.title = title;
            this.screen = screen;
        }

    }

    public static class Adapter extends ArrayAdapter<Item> {

        public Adapter(Context context) {
            super(context, -1);
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

        public void load(Collection<PluginInfo> infos) {
            add(new Item(Item.Type.HEADER, getContext().getString(R.string.drawer_device), null));
            add(new Item(Item.Type.ITEM, getContext().getString(R.string.music), new GalleryScreen()));
//            add(new Item(Item.Type.ITEM, context.getString(R.string.folders), new FolderScreen()));
            if (infos != null) {
                add(new Item(Item.Type.HEADER, getContext().getString(R.string.drawer_library), null));
                for (final PluginInfo info : infos) {
//                add(new Item(Item.Type.ITEM, info.title, new LibraryScreen(info)));
                }
            }
            add(new Item(Item.Type.HEADER, getContext().getString(R.string.menu_settings), null));
        }

    }

}
