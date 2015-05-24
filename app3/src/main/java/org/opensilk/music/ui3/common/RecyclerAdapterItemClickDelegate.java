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
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.opensilk.common.core.rx.SimpleObserver;

import java.util.Map;
import java.util.WeakHashMap;

import rx.Observer;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;

/**
 * Created by drew on 5/24/15.
 */
public class RecyclerAdapterItemClickDelegate<VH extends RecyclerView.ViewHolder & RecyclerAdapterItemClickDelegate.ClickableItem> {

    public interface ClickableItem {
        View getContentView();
    }

    public interface ItemClickListener {
        void onItemClicked(Context context, int pos);
    }

    final Map<View, SubCont> itemClickSubscriptions = new WeakHashMap<>();
    final ItemClickListener listener;

    public RecyclerAdapterItemClickDelegate(ItemClickListener listener) {
        this.listener = listener;
    }

    public void bindClickListener(VH holder, int position) {
        itemClickSubscriptions.put(holder.getContentView(),
                SubCont.ni(position, ViewObservable.clicks(holder.getContentView()).subscribe(itemClickObserver)));
    }

    public void onViewRecycled(VH holder) {
        SubCont c = itemClickSubscriptions.remove(holder.getContentView());
        if (c != null) {
            c.sub.unsubscribe();
        }
    }

    final Observer<OnClickEvent> itemClickObserver = new SimpleObserver<OnClickEvent>() {
        @Override
        public void onNext(OnClickEvent onClickEvent) {
            SubCont c = itemClickSubscriptions.get(onClickEvent.view);
            if (c != null) {
                Context context = onClickEvent.view.getContext();
                listener.onItemClicked(context, c.pos);
            }
        }
    };

    static class SubCont {
        final int pos;
        final Subscription sub;
        public SubCont(int pos, Subscription sub) {
            this.pos = pos;
            this.sub = sub;
        }
        public static SubCont ni(int pos, Subscription sub) {
            return new SubCont(pos, sub);
        }
    }
}
