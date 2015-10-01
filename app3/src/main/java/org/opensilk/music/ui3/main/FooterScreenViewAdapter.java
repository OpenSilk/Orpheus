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

import android.content.Context;
import android.support.annotation.NonNull;

import org.opensilk.common.ui.mortar.MortarPagerAdapter;

import java.util.List;

/**
 * Created by drew on 9/17/15.
 */
public class FooterScreenViewAdapter extends MortarPagerAdapter<FooterPageScreen, FooterPageScreenView> {

    public FooterScreenViewAdapter(Context context) {
        super(context);
    }

    public FooterScreenViewAdapter(@NonNull Context context, @NonNull List<FooterPageScreen> screens) {
        super(context, screens);
    }

    public static FooterScreenViewAdapter create(Context context, List<FooterPageScreen> screens) {
        return new FooterScreenViewAdapter(context, screens);
    }
}
