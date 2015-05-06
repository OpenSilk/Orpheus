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

package org.opensilk.music.ui3.profile;

import android.content.Context;

import org.opensilk.common.core.mortar.HasScope;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;

/**
 * Created by drew on 11/21/14.
 */
public interface ProfileView extends HasScope {
    BundleableRecyclerAdapter getAdapter();
    Context getContext();
    boolean isLandscape();
    void prepareRefresh();
}
