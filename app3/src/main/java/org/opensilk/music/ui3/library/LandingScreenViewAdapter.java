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

package org.opensilk.music.ui3.library;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;

import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

import rx.Observer;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.AndroidObservable;
import rx.android.observables.ViewObservable;
import rx.android.subscriptions.AndroidSubscriptions;

/**
 * Created by drew on 5/1/15.
 */
public class LandingScreenViewAdapter extends
        RecyclerListAdapter<LandingScreenViewAdapter.ViewItem, LandingScreenViewAdapter.ViewHolder>
        implements Observer<OnClickEvent> {

    final LandingScreenPresenter presenter;
    final Map<View, SubscriptionContainer> itemClickSubscriptions = new WeakHashMap<>();

    @Inject
    public LandingScreenViewAdapter(LandingScreenPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, android.R.layout.simple_list_item_1));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ViewItem item = getItem(position);
        ((TextView) holder.itemView).setText(item.text);
        itemClickSubscriptions.put(holder.itemView,
                new SubscriptionContainer(position, ViewObservable.clicks(holder.itemView).subscribe(this)));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        SubscriptionContainer c = itemClickSubscriptions.remove(holder.itemView);
        if (c != null) {
            c.s.unsubscribe();
        }
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(OnClickEvent onClickEvent) {
        SubscriptionContainer c = itemClickSubscriptions.get(onClickEvent.view);
        if (c != null) {
            presenter.onItemClicked(onClickEvent.view.getContext(), getItem(c.pos));
        }
    }

    static class SubscriptionContainer{
        final int pos;
        final Subscription s;

        public SubscriptionContainer(int pos, Subscription s) {
            this.pos = pos;
            this.s = s;
        }
    }

    public static class ViewItem {
        public static final ViewItem ALBUMS = new ViewItem("Albums");
        public static final ViewItem ARTISTS = new ViewItem("Artists");
        public static final ViewItem FOLDERS = new ViewItem("Folders");
        public static final ViewItem GENRES = new ViewItem("Genres");
        public static final ViewItem PLAYLISTS = new ViewItem("Playlists");
        public static final ViewItem TRACKS = new ViewItem("Tracks");
        final String text;

        public ViewItem(String text) {
            this.text = text;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
