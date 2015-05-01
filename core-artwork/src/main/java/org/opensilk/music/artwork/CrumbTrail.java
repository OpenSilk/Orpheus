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

package org.opensilk.music.artwork;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by drew on 4/30/15.
 */
public class CrumbTrail {
    final ArrayList<String> breadcrumbs = new ArrayList<>();
    boolean followed = false;

    public void drop(String msg) {
        breadcrumbs.add(msg);
    }

    public void follow() {
        followed = true;
        Timber.v(assembleTrail());
    }

    private String assembleTrail() {
        StringBuilder b = new StringBuilder(500);
        for (int ii=0; ii<breadcrumbs.size(); ii++) {
            b.append(breadcrumbs.get(ii));
            if (ii < breadcrumbs.size()-1)
                b.append(" -> ");
        }
        return b.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        if (!followed) {
            Timber.e("CrumbTrail never followed: %s", assembleTrail());
        }
        super.finalize();
    }
}
