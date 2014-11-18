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

import com.google.gson.Gson;

import org.opensilk.common.flow.GsonParcer;
import org.opensilk.common.mortar.PauseAndResumeActivity;
import org.opensilk.common.mortar.PauseAndResumeModule;
import org.opensilk.common.mortar.PauseAndResumePresenter;
import org.opensilk.common.util.ObjectUtils;
import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.loader.LoaderModule;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Parcer;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 11/7/14.
 */
public class BaseMortarActivity extends BaseActivity implements PauseAndResumeActivity {

    public static abstract class Blueprint implements mortar.Blueprint {
        /**
         * Required for a race condition cause by Android when a new scope is created
         * before the old one is destroyed
         * <p/>
         * https://github.com/square/mortar/issues/87#issuecomment-43849264
         */
        private final String scopeName;

        public Blueprint(String scopeName) {
            this.scopeName = scopeName;
        }

        @Override public String getMortarScopeName() {
            return scopeName;
        }

    }

    @dagger.Module(
            includes = {
                    BaseActivity.Module.class,
                    PauseAndResumeModule.class
            }
    )
    public static class Module {

    }

    @Inject PauseAndResumePresenter mPauseResumePresenter;

    protected MortarActivityScope mActivityScope;
    protected String mScopeName;

    protected mortar.Blueprint getBlueprint(String scopeName) {
        throw new UnsupportedOperationException("Subclass must override getBlueprint()");
    }

    protected void setupTheme() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivityScope = Mortar.requireActivityScope(Mortar.getScope(getApplication()), getBlueprint(getScopeName()));
        mActivityScope.onCreate(savedInstanceState);
        Mortar.inject(this, this); //Must inject before calling super
        setupTheme();
        super.onCreate(savedInstanceState);

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
