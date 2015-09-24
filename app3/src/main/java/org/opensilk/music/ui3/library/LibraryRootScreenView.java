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
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
public class LibraryRootScreenView extends CardView {

    @Inject LibraryRootScreenPresenter mPresenter;

    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.avatar) ImageView avatar;
    @InjectView(R.id.loading_progress) ProgressBar progress;
    @InjectView(R.id.btn_login) Button loginBtn;
    @InjectView(R.id.btn_retry) Button retryBtn;
    @InjectView(R.id.tile_overflow) View overflow;
    @InjectView(R.id.roots_container) ViewGroup rootsContainer;

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
                        //TODO;
                    }
                }));
        clicksSubs.add(RxView.clickEvents(loginBtn)
                .subscribe(new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent viewClickEvent) {

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

    void addRoots(List<Container> roots) {
        clearRoots();
        for (final Container c : roots) {
            TextView tv = ViewUtils.inflate(rootsContainer.getContext(),
                    R.layout.mtrl_list_item_oneline_text, rootsContainer, false);
            tv.setText(c.getDisplayName());
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
        setRetry();
    }

    void setloading() {
        progress.setVisibility(VISIBLE);
        loginBtn.setVisibility(GONE);
        retryBtn.setVisibility(GONE);
    }

    void setNeedsAuth() {
        progress.setVisibility(GONE);
        loginBtn.setVisibility(VISIBLE);
        retryBtn.setVisibility(GONE);
        clearRoots();
    }

    void setRetry() {
        progress.setVisibility(GONE);
        loginBtn.setVisibility(GONE);
        retryBtn.setVisibility(VISIBLE);
    }
}
