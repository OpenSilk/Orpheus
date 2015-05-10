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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import org.opensilk.common.core.mortar.DaggerService;

import javax.inject.Inject;

/**
 * Created by drew on 5/9/15.
 */
public class QueueScreenView extends RelativeLayout {

    @Inject QueueScreenPresenter mPresenter;

    public QueueScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        QueueScreenComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

}
