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
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.opensilk.music.api.meta.PluginInfo;

import java.util.Collection;

import javax.inject.Inject;

import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class NavView extends ListView {

    @Inject NavPresenter presenter;

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
        final NavAdapter adapter = new NavAdapter(getContext(), infos);
        setAdapter(adapter);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NavItem item = adapter.getItem(position);
                if (item.screen != null) {
                    setItemChecked(position, true);
                    presenter.go(getContext(), item.screen);
                } else if (item.event != null) {
                    presenter.open(item.event);
                }
            }
        });
    }

}
