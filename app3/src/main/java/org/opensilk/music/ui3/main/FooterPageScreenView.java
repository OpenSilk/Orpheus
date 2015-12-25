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

package org.opensilk.music.ui3.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.view.ViewClickEvent;
import com.jakewharton.rxbinding.view.ViewLongClickEvent;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.widget.ImageButtonCheckable;
import org.opensilk.music.R;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 9/17/15.
 */
public class FooterPageScreenView extends LinearLayout {

    @Inject FooterPageScreenPresenter mPresenter;

    @InjectView(R.id.footer_track_title) TextView trackTitle;
    @InjectView(R.id.footer_artist_name) TextView artistName;
    @InjectView(R.id.footer_thumbnail) ImageView artworkThumbnail;
    @InjectView(R.id.footer_playpause_btn) ImageButtonCheckable mPlayPause;

    CompositeSubscription mSubs = new CompositeSubscription();

    public FooterPageScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            FooterPageScreenComponent cmp = DaggerService.getDaggerComponent(getContext());
            cmp.inject(this);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            ButterKnife.inject(this);
            if (VersionUtils.hasLollipop()) {
                setupDrawables21();
            }
            mPresenter.takeView(this);
            subscribeClicks();
        }
    }

    @TargetApi(21)
    void setupDrawables21() {
        AnimatedStateListDrawable drawable = (AnimatedStateListDrawable) mPlayPause.getDrawable();
        drawable.addTransition(R.id.pause_state, R.id.play_state, (AnimatedVectorDrawable)
                ContextCompat.getDrawable(getContext(), R.drawable.vector_pause_play_black_36dp), false);
        drawable.addTransition(R.id.play_state, R.id.pause_state, (AnimatedVectorDrawable)
                ContextCompat.getDrawable(getContext(), R.drawable.vector_play_pause_black_36dp), false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mPresenter.takeView(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    void setPlaying(boolean yes) {
        mPlayPause.setChecked(yes);
    }

    void subscribeClicks() {
        mSubs.add(RxView.clickEvents(this).subscribe(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent viewClickEvent) {
                mPresenter.openNowPlaying(viewClickEvent.view());
            }
        }));
        mSubs.add(RxView.clickEvents(mPlayPause).subscribe(new Action1<ViewClickEvent>() {
            @Override
            public void call(ViewClickEvent viewClickEvent) {
                mPresenter.togglePlayback();
            }
        }));
        mSubs.add(RxView.longClickEvents(mPlayPause, new Func1<ViewLongClickEvent, Boolean>() {
            @Override
            public Boolean call(ViewLongClickEvent viewLongClickEvent) {
                return true;
            }
        }).subscribe(new Action1<ViewLongClickEvent>() {
            @Override
            public void call(ViewLongClickEvent viewLongClickEvent) {
                mPresenter.openControls();
            }
        }));
    }
}
