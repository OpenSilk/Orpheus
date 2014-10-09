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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew.apollo.R;
import com.etsy.android.grid.StaggeredGridView;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.ui2.main.DrawerPresenter;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;

/**
 * Created by drew on 10/3/14.
 */
public class AlbumView extends StaggeredGridView {

    @Inject
    AlbumScreen.Presenter presenter;

    public AlbumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        presenter.takeView(this);
        setColumnCount(1);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getContext(), "Clicked " + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    public Adapter getAdapter() {
        return (Adapter) super.getAdapter();
    }

    public void makeAdapter(List<Album> albums) {
        setAdapter(new Adapter(getContext(), albums));
    }

    class Adapter extends ArrayAdapter<Album> {

        private Adapter(Context context, List<Album> albums) {
            super(context, -1, albums);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Album album = getItem(position);
            final Holder holder;

            if (convertView != null) {
                holder = (Holder) convertView.getTag();
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listcard_artwork_inner, parent, false);
                holder = new Holder(convertView);
                convertView.setTag(holder);
            }

            holder.title.setText(album.name);
            holder.subTitle.setText(album.artistName);
            holder.overflow.setOnClickListener(presenter.makeOverflowListener(getContext(), album));

            return convertView;
        }
    }

    static class Holder {
        @InjectView(R.id.card_title)
        TextView title;
        @InjectView(R.id.card_subtitle)
        TextView subTitle;
        @InjectView(R.id.card_overflow_button)
        View overflow;

        private Holder(View view) {
            ButterKnife.inject(this, view);
        }
    }

}
