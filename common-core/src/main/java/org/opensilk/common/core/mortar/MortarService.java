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

package org.opensilk.common.core.mortar;

import android.app.Service;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 3/21/15.
 */
public abstract class MortarService extends Service {

    protected MortarScope mServiceScope;

    protected abstract void onBuildScope(MortarScope.Builder builder);

    @Override
    public void onCreate() {
        super.onCreate();
        ensureScope();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mServiceScope != null && !mServiceScope.isDestroyed()) {
            Timber.d("Destroying service scope %s", getScopeName());
            mServiceScope.destroy();
            mServiceScope = null;
        }
    }

    @Override
    public Object getSystemService(String name) {
        ensureScope();
        return mServiceScope.hasService(name) ? mServiceScope.getService(name) : super.getSystemService(name);
    }

    protected String getScopeName() {
        return getClass().getName();
    }

    protected void ensureScope() {
        if (mServiceScope == null) {
            mServiceScope = MortarScope.findChild(getApplicationContext(), getScopeName());
        }
        if (mServiceScope == null) {
            MortarScope.Builder builder = MortarScope.getScope(getApplicationContext()).buildChild();
            onBuildScope(builder);
            mServiceScope = builder.build(getScopeName());
        }
    }
}
