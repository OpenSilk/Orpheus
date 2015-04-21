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

package org.opensilk.iab.gplay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.FrameLayout;

import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.iab.gplay.util.IabHelper;
import org.opensilk.iab.gplay.util.IabResult;

import javax.inject.Inject;

import dagger.ObjectGraph;

import static org.opensilk.iab.core.DonateManager.D;

/**
 * Created by drew on 4/21/15.
 */
public class DonateActivity extends ActionBarActivity implements DaggerInjector {
    private static final String TAG = "PurchaseActivity";

    static final int PURCHASE_REQ = 1223;

    ObjectGraph mGraph;
    boolean mConfigurationChangeIncoming;

    @Inject IabHelper mIabHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean fromConfigurationChange = false;
        mGraph = (ObjectGraph) getLastCustomNonConfigurationInstance();
        if (mGraph == null) {
            if (D) Log.d(TAG, "Creating new graph");
            mGraph = ((DaggerInjector) getApplication()).getObjectGraph().plus(new DonateActivityModule());
        } else {
            fromConfigurationChange = true;
        }
        inject(this);
        super.onCreate(savedInstanceState);

        //TODO is this even needed?
        FrameLayout fl = new FrameLayout(this);
        fl.setId(R.id.iab_main);
        setContentView(fl);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        if (!fromConfigurationChange) {
            getSupportFragmentManager().beginTransaction()
                    .add(new PurchaseFragment(), "purchase")
                    .commit();
            mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (D) Log.d(TAG, "onSetupFinished");
                    Fragment f = getSupportFragmentManager().findFragmentByTag("donate");
                    if (f == null) {
                        f = DonateFragment.newInstance(result.isSuccess());
                    }
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.iab_main, f, "donate")
                            .commit();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mConfigurationChangeIncoming) {
            if (mIabHelper != null) {
                mIabHelper.dispose();
            }
        }
        mIabHelper = null;
        mGraph = null;
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        mConfigurationChangeIncoming = true;
        return mGraph;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mIabHelper != null && mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (super.onSupportNavigateUp()) {
            return true;
        } else {
            finish();
            return true;
        }
    }

    @Override
    public void inject(Object obj) {
        mGraph.inject(obj);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mGraph;
    }

}
