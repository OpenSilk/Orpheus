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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opensilk.music.R;
import org.opensilk.common.glide.Paletteable;

import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 10/31/14.
 */
public class GridTileDescription extends LinearLayout implements Paletteable {

    @InjectView(R.id.tile_title) TextView mTitle;
    @InjectView(R.id.tile_subtitle) TextView mSubTitle;

    public GridTileDescription(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public TextView getTitle() {
        return mTitle;
    }

    public TextView getSubTitle() {
        return mSubTitle;
    }

    @Override
    public List<? extends View> getBackgroundViews() {
        return Collections.singletonList(this);
    }

    @Override
    public List<TextView> getTitleViews() {
        return Collections.singletonList(mTitle);
    }

    @Override
    public List<TextView> getBodyViews() {
        return Collections.singletonList(mSubTitle);
    }
}
