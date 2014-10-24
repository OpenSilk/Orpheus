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

package org.opensilk.music;

import android.app.Activity;
import android.os.Bundle;

import org.opensilk.music.ui2.ActivityBlueprint;
import org.opensilk.music.ui2.main.MainViewBlueprint;

import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;

/**
 * Created by drew on 10/19/14.
 */
public class MortarTestActivity extends Activity {

    public MortarActivityScope mActivityScope;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MortarScope parentScope = Mortar.getScope(getApplication());
        mActivityScope = Mortar.requireActivityScope(parentScope, getStartingScope());
        mActivityScope.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            if (mActivityScope != null && !mActivityScope.isDestroyed()) {
                MortarScope parentScope = Mortar.getScope(getApplication());
                parentScope.destroyChild(mActivityScope);
            }
            mActivityScope = null;
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mActivityScope;
        }
        return super.getSystemService(name);
    }

    protected String getScopeName() {
        return MortarTestActivity.class.getName();
    }

    protected Blueprint getStartingScope() {
        return new ActivityBlueprint(getScopeName());
    }

}
