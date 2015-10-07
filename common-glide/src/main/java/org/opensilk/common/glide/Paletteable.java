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

package org.opensilk.common.glide;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import java.util.List;

/**
 * Created by drew on 10/6/15.
 */
public interface Paletteable {
    @Nullable List<? extends View> getBackgroundViews();
    @Nullable List<TextView> getTitleViews();
    @Nullable List<TextView> getBodyViews();
}
