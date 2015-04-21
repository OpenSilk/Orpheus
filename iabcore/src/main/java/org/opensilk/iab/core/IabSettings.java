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

package org.opensilk.iab.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 4/21/15.
 */
@Singleton
public class IabSettings {
    public static final String NAME = "iab";

    public static final String PREF_APP_LAUNCHES = "app_launches";
    public static final String PREF_NEXT_BOTHER = "iab_next_bother";

    private static final long ONE_MINUTE_MILLI = 60 * 1000;
    private static final long ONE_HOUR_MILLI = 60 * ONE_MINUTE_MILLI;
    private static final long ONE_DAY_MILLI = 24 * ONE_HOUR_MILLI;
    private static final long ONE_WEEK_MILLI = 7 * ONE_DAY_MILLI;
    private static final long MIN_INTERVAL_FOR_BOTHER = 2 * ONE_WEEK_MILLI;
    private static final int MIN_LAUNCHES_FOR_BOTHER = 4;

    private final SharedPreferences prefs;

    @Inject
    public IabSettings(@ForApplication Context appContext) {
        this.prefs = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void incrementAppLaunchCount() {
        int prevCount = getInt(PREF_APP_LAUNCHES, 0) + 1;
        putInt(PREF_APP_LAUNCHES, prevCount);
    }

    public boolean shouldBother() {
        long nextBother = getLong(PREF_NEXT_BOTHER, 0);
        int openCount = getInt(PREF_APP_LAUNCHES, 0);
        if (openCount >= MIN_LAUNCHES_FOR_BOTHER && nextBother <= System.currentTimeMillis()) {
            putInt(PREF_APP_LAUNCHES, 0);
            putLong(PREF_NEXT_BOTHER, System.currentTimeMillis() + MIN_INTERVAL_FOR_BOTHER);
            return true;
        }
        return false;
    }

    int getInt(String pref, int def) {
        return prefs.getInt(pref, def);
    }

    void putInt(String pref, int val) {
        prefs.edit().putInt(pref, val).apply();
    }

    long getLong(String pref, long def) {
        return prefs.getLong(pref, def);
    }

    void putLong(String pref, long val) {
        prefs.edit().putLong(pref, val).apply();
    }

}
