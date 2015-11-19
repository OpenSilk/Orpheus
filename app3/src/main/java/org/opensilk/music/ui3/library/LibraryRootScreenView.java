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

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.util.ViewUtils;
import org.opensilk.music.R;
import org.opensilk.music.model.Container;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/23/15.
 */
public class LibraryRootScreenView extends FrameLayout {

    @Inject LibraryRootScreenPresenter mPresenter;

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.avatar) ImageView avatar;
    @InjectView(R.id.loading_progress) ProgressBar progress;
    @InjectView(R.id.btn_retry) ImageButton retryBtn;
    @InjectView(R.id.tile_overflow) View overflow;
    @InjectView(R.id.roots_container) ViewGroup rootsContainer;
    @InjectView(R.id.error_msg) TextView errorMsg;

    final CompositeSubscription clicksSubs = new CompositeSubscription();

    public LibraryRootScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            LibraryRootScreenComponent cmp = DaggerService.getDaggerComponent(getContext());
            cmp.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if(!isInEditMode()) {
            ButterKnife.inject(this);
            mPresenter.takeView(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mPresenter.takeView(this);
        }
        subscribeClicks();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
        clicksSubs.clear();
    }

    void subscribeClicks() {
        clicksSubs.add(RxView.clickEvents(overflow)
                .subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        final View view = viewClickEvent.view();
                        PopupMenu popupMenu = new PopupMenu(getContext(), view);
                        mPresenter.populateMenu(getContext(), popupMenu);
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                mPresenter.handlePopupItemClick(getContext(), item);
                                return true;
                            }
                        });
                        popupMenu.show();
                    }
                }));
        clicksSubs.add(RxView.clickEvents(retryBtn)
                .subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {
                        mPresenter.reload();
                    }
                }));
    }

    void clearRoots() {
        if (rootsContainer.getChildCount() > 0) {
            clicksSubs.clear();
            rootsContainer.removeAllViews();
            subscribeClicks();
        }
    }

    void addRoots(List<Container> roots, boolean clear) {
        if (clear) {
            clearRoots();
        }
        for (final Container c : roots) {
            TextView tv = ViewUtils.inflate(rootsContainer.getContext(),
                    R.layout.mtrl_list_item_oneline_text, rootsContainer, false);
            tv.setText(c.getName());
            clicksSubs.add(RxView.clickEvents(tv).subscribe(
                    new Action1<ViewClickEvent>() {
                        @Override
                        public void call(ViewClickEvent onClickEvent) {
                            mPresenter.getParent().onRootClick(onClickEvent.view(), c);
                        }
                    }
            ));
            rootsContainer.addView(tv);
        }
        showRetry();
    }

    void setloading() {
        progress.setVisibility(VISIBLE);
        errorMsg.setVisibility(GONE);
        retryBtn.setVisibility(GONE);
    }

    void setNeedsAuth() {
        progress.setVisibility(GONE);
        errorMsg.setText(R.string.err_authentication);
        errorMsg.setVisibility(VISIBLE);
        retryBtn.setVisibility(GONE);
        clearRoots();
    }

    void showRetry() {
        progress.setVisibility(GONE);
        errorMsg.setVisibility(GONE);
        retryBtn.setVisibility(VISIBLE);
    }

    void setError(String msg) {
        showRetry();
        clearRoots();
        errorMsg.setText(msg);
        errorMsg.setVisibility(VISIBLE);
    }

    void setUnavailable() {
        setError(getContext().getString(R.string.err_library_unavailable));
    }
}
