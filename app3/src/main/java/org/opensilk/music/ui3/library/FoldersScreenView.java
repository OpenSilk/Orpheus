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
import android.util.AttributeSet;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.widget.FloatingActionButtonCheckable;
import org.opensilk.music.R;
import org.opensilk.music.model.Container;
import org.opensilk.music.ui3.common.BundleableRecyclerCoordinator;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/17/15.
 */
public class FoldersScreenView extends BundleableRecyclerCoordinator {

    @Inject @Named("folders_title") String mTitle;
    @Inject Container mThisContainer;

    @InjectView(R.id.floating_action_button) FloatingActionButtonCheckable mFab;

    CompositeSubscription mSubscriptions = new CompositeSubscription();

    public FoldersScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void inject() {
        FoldersScreenComponent cmp = DaggerService.getDaggerComponent(getContext());
        cmp.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        updateFab();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscribeClicks();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSubscriptions.clear();
    }

    void updateFab() {
        mFab.setChecked(mPresenter.getIndexClient().isIndexed(mThisContainer));
    }

    void subscribeClicks() {
        mSubscriptions.add(RxView.clickEvents(mFab).subscribe(
                new Action1<ViewClickEvent>() {
                    @Override
                    public void call(ViewClickEvent onClickEvent) {
                        if (!mPresenter.getIndexClient().isIndexed(mThisContainer)) {
                            mPresenter.getIndexClient().add(mThisContainer);
                            mFab.setChecked(true);
                        } else {
                            mPresenter.getIndexClient().remove(mThisContainer);
                            //TODO show toast
                            mFab.setChecked(false);
                        }
                    }
                }
        ));
    }

}
