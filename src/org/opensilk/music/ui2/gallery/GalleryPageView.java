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

import org.lucasr.twowayview.TwoWayView;

import mortar.ViewPresenter;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
public class GalleryPageView extends TwoWayView {

    ViewPresenter<GalleryPageView> presenter;

    public GalleryPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Timber.v("onAttachedToWindow");
        if (presenter != null) {
            presenter.takeView(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Timber.v("onDetachedFromWindow");
        if (presenter != null) {
            presenter.dropView(this);
        }
    }

    // Set this right after inflate, before adding to the container
    public void setPresenter(ViewPresenter<GalleryPageView> presenter) {
        this.presenter = presenter;
    }

    public ViewPresenter<GalleryPageView> getPresenter() {
        return presenter;
    }
}
