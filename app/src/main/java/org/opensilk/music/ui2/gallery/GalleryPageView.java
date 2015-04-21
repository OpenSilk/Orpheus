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
import android.os.Parcelable;
import android.util.AttributeSet;

import org.opensilk.music.widgets.RecyclerListFrame;

import javax.inject.Inject;

import butterknife.ButterKnife;
import mortar.Mortar;
import mortar.ViewPresenter;

/**
 * Created by drew on 10/21/14.
 */
public class GalleryPageView extends RecyclerListFrame {

    @Inject ViewPresenter<GalleryPageView> presenter;

    public GalleryPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        Timber.v("onFinishInflate()");
        ButterKnife.inject(this);
        //note dont remove, needs to be called here for adapter savedstate
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        Timber.v("onAttachedToWindow()");
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        Timber.v("onDetachedFromWindow");
        presenter.dropView(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
//        Timber.v("onSaveInstanceState");
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
//        Timber.v("onRestoreInstanceState");
        super.onRestoreInstanceState(state);
    }

    public ViewPresenter<GalleryPageView> getPresenter() {
        return presenter;
    }

}
