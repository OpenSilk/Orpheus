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

package org.opensilk.common.core.app;

import android.app.Activity;
import android.os.Bundle;

import org.opensilk.common.core.mortar.HasScope;
import org.opensilk.common.core.util.ObjectUtils;

import java.util.UUID;

import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;
import timber.log.Timber;

/**
 * Created by drew on 4/29/15.
 */
public abstract class MortarActivity extends Activity implements HasScope {
    private String mScopeName;
    protected MortarScope mActivityScope;
    protected boolean mIsResumed;
    protected boolean mConfigurationChangeIncoming;

    protected abstract void onCreateScope(MortarScope.Builder builder);

    /**
     * Allows abstract subclasses to inject additional services
     * without worrying about their concrete offspring forgetting
     * to call super
     */
    protected void onPreCreateScope(MortarScope.Builder buidler) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivityScope = MortarScope.findChild(getApplicationContext(), getScopeName());
        if (mActivityScope == null) {
            MortarScope.Builder builder = MortarScope.buildChild(getApplicationContext())
                    .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner());
            onPreCreateScope(builder);
            onCreateScope(builder);
            mActivityScope = builder.build(getScopeName());
            Timber.d("Created new scope %s", mActivityScope.getName());
        } else {
            Timber.d("Reusing old scope %s", mActivityScope.getName());
        }
        super.onCreate(savedInstanceState);
        BundleServiceRunner.getBundleServiceRunner(this).onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BundleServiceRunner.getBundleServiceRunner(this).onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mConfigurationChangeIncoming) {
            // Destroy our scope
            if (mActivityScope != null && !mActivityScope.isDestroyed()) {
                Timber.d("Destroying activity scope");
                mActivityScope.destroy();
            }
            mActivityScope = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mConfigurationChangeIncoming = true;
        return mScopeName;
    }

    @Override
    public Object getSystemService(String name) {
        return (mActivityScope != null && mActivityScope.hasService(name))
                ? mActivityScope.getService(name) : super.getSystemService(name);
    }

    @Override
    public MortarScope getScope() {
        return mActivityScope;
    }

    private String getScopeName() {
        if (mScopeName == null) {
            mScopeName = (String) getLastNonConfigurationInstance();
        }
        if (mScopeName == null) {
            mScopeName = ObjectUtils.<MortarActivity>getClass(this).getSimpleName() + "-" + UUID.randomUUID().toString();
        }
        return mScopeName;
    }
}
