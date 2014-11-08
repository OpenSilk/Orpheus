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

package org.opensilk.music.ui2;

import android.os.Bundle;

import org.opensilk.common.mortar.PauseAndResumeActivity;
import org.opensilk.common.mortar.PauseAndResumePresenter;
import org.opensilk.common.util.ObjectUtils;

import java.util.UUID;

import javax.inject.Inject;

import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 11/7/14.
 */
public class BaseMortarActivity extends BaseActivity implements PauseAndResumeActivity {

    @Inject PauseAndResumePresenter mPauseResumePresenter;

    protected MortarActivityScope mActivityScope;
    protected String mScopeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityScope = Mortar.requireActivityScope(Mortar.getScope(getApplication()), getBlueprint(getScopeName()));
        mActivityScope.onCreate(savedInstanceState);
        Mortar.inject(this, this);

        mPauseResumePresenter.takeView(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPauseResumePresenter != null) mPauseResumePresenter.dropView(this);
        if (!mConfigurationChangeIncoming) {
            // Destroy our scope
            if (mActivityScope != null && !mActivityScope.isDestroyed()) {
                MortarScope parentScope = Mortar.getScope(getApplication());
                parentScope.destroyChild(mActivityScope);
            }
            mActivityScope = null;
        }
    }

    @Override
    protected void onResume() {
        Timber.v("onResume()");
        super.onResume();
        if (mPauseResumePresenter != null) mPauseResumePresenter.activityResumed();
    }

    @Override
    protected void onPause() {
        Timber.v("onPause()");
        super.onPause();
        if (mPauseResumePresenter != null) mPauseResumePresenter.activityPaused();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActivityScope.onSaveInstanceState(outState);
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mActivityScope;
        }
        return super.getSystemService(name);
    }

    /*
     * PausesAndResumes
     */

    @Override
    public boolean isRunning() {
        return mIsResumed;
    }

    /*
     * HasScope
     */

    public MortarScope getScope() {
        return mActivityScope;
    }

    @Override
    protected Object getObjectForRetain() {
        return mActivityScope.getName();
    }

    protected Blueprint getBlueprint(String scopeName) {
        return new ActivityBlueprint(scopeName);
    }

    private String getScopeName() {
        if (mScopeName == null) {
            mScopeName = (String) getLastCustomNonConfigurationInstance();
        }
        if (mScopeName == null) {
            mScopeName = ObjectUtils.<LauncherActivity>getClass(this).getName() + UUID.randomUUID().toString();
        }
        return mScopeName;
    }
}
