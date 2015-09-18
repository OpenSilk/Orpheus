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

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.spi.Bundleable;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/6/15.
 */
public class LibraryScreenViewAdapter extends RecyclerListAdapter<ProviderInfoItem, LibraryScreenViewAdapter.ViewHolder> {

    final LibraryScreenPresenter presenter;

    @Inject
    public LibraryScreenViewAdapter(
            LibraryScreenPresenter presenter
    ) {
        this.presenter = presenter;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, viewType));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        ProviderInfoItem item = getItem(position);
        LibraryProviderInfo info = item.getInfo();
        holder.title.setText(info.getTitle());
        Drawable d = info.getIcon();
        if (d == null) {
            d = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_extension_grey600_24dp);
        }
//            int bounds = (int) (24 * holder.itemView.getResources().getDisplayMetrics().density);
//            d.setBounds(0, 0, bounds, bounds);
        holder.avatar.setImageDrawable(d);
        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
        holder.progress.setVisibility(item.isLoading() ? View.VISIBLE : View.GONE);
        holder.loginBtn.setVisibility(item.needsLogin() ? View.VISIBLE : View.GONE);
        if (item.needsLogin()) {
            holder.loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        } else if (!item.isLoading()) {
            holder.addRoots(item.getRoots(), presenter);
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.library_provider_item_view;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.avatar) ImageView avatar;
        @InjectView(R.id.loading_progress) ProgressBar progress;
        @InjectView(R.id.btn_login) Button loginBtn;
        @InjectView(R.id.tile_overflow) View overflow;
        @InjectView(R.id.roots_container) ViewGroup rootsContainer;
        CompositeSubscription subscriptions = new CompositeSubscription();

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            reset();
        }

        void addRoots(List<Container> roots, final LibraryScreenPresenter presenter) {
            for (final Container c : roots) {
                TextView tv = ViewUtils.inflate(rootsContainer.getContext(),
                        R.layout.mtrl_list_item_oneline_text, rootsContainer, false);
                tv.setText(c.getDisplayName());
                subscriptions.add(RxView.clickEvents(tv).subscribe(
                        new Action1<ViewClickEvent>() {
                            @Override
                            public void call(ViewClickEvent onClickEvent) {
                                presenter.onRootClick(onClickEvent.view(), c);
                            }
                        }
                ));
                rootsContainer.addView(tv);
            }
        }

        void reset() {
            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (loginBtn != null) loginBtn.setVisibility(View.GONE);
            if (rootsContainer != null) rootsContainer.removeAllViews();
            subscriptions.clear();
        }
    }
}
