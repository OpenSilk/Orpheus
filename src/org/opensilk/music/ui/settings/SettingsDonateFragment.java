/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;

import org.opensilk.music.iab.IabHelper;
import org.opensilk.music.iab.IabUtil;
import org.opensilk.music.iab.IabResult;
import org.opensilk.music.iab.Inventory;
import org.opensilk.music.iab.Purchase;
import org.opensilk.music.iab.SkuDetails;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 4/26/14.
 */
public class SettingsDonateFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {
    protected static final String TAG = SettingsDonateFragment.class.getSimpleName();

    protected PreferenceScreen mPrefscreen;
    protected IabHelper mIabHelper;

    protected ProgressDialog mWaitDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_donate);
        mPrefscreen = getPreferenceScreen();
        mIabHelper = IabUtil.newHelper(getActivity());
        mIabHelper.enableDebugLogging(BuildConfig.DEBUG);
        createWaitDialog();
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @DebugLog
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    Log.d(TAG, "result success");
                    if (mIabHelper != null) {
                        mIabHelper.queryInventoryAsync(true, IabUtil.PRODUCT_SKUS, mQueryListener);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.iab_service_error, Toast.LENGTH_LONG).show();
                    getActivity().onBackPressed();
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIabHelper != null) {
            mIabHelper.dispose();
            mIabHelper = null;
        }
    }

    @Override
    @DebugLog
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mIabHelper != null && mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            Log.d(TAG, "IABHelper handled activity result");
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @DebugLog
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof PurchasePreference) {
            PurchasePreference p = (PurchasePreference) preference;
            if (p.isPurchased) {
                Log.e(TAG, "Whoa already purchased");
                return false;
            }
            switch (p.getKey()) {
                case IabUtil.SKU_DONATE_ONE:
                case IabUtil.SKU_DONATE_TWO:
                    showPurchaseDialog(p.skuDetails);
                    break;
                default:
                    Log.e(TAG, "WTF why is there an unknown preference!");
                    break;
            }
        }
        return false;
    }

    private void createWaitDialog() {
        mWaitDialog = new ProgressDialog(getActivity());
        mWaitDialog.setIndeterminate(true);
        mWaitDialog.setMessage(getString(R.string.iab_fetching_inventory));
        mWaitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                getActivity().onBackPressed();
            }
        });
        mWaitDialog.show();
    }

    private void showPurchaseDialog(final SkuDetails details) {
        new AlertDialog.Builder(getActivity())
                .setTitle(details.getTitle())
                .setMessage(details.getDescription())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doPurchase(details.getSku());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doPurchase(String sku) {
        mIabHelper.launchPurchaseFlow(getActivity(), sku, 1001, mPurchaseListener, IabUtil.developerPayload);
    }

    private PurchasePreference addPreference(SkuDetails details) {
        PurchasePreference p = new PurchasePreference(getActivity(), details);
        p.setOnPreferenceClickListener(this);
        mPrefscreen.addPreference(p);
        return p;
    }

    private final IabHelper.QueryInventoryFinishedListener mQueryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        @DebugLog
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            mWaitDialog.dismiss();
            if (result.isSuccess()) {
                Log.d(TAG, "result success");
                for (String sku : IabUtil.PRODUCT_SKUS) {
                    Purchase p = inv.getPurchase(sku);
                    if (p != null && IabUtil.verifyDeveloperPayload(p)) {
                        Log.d(TAG, "purchased: " + p.toString());
                        final PurchasePreference pref = addPreference(inv.getSkuDetails(p.getSku()));
                        pref.setSummary(R.string.iab_purchased);
                        pref.setEnabled(false);
                        pref.isPurchased = true;
                        //XXX for testing only
                        mIabHelper.consumeAsync(p, new IabHelper.OnConsumeFinishedListener() {
                            @Override
                            @DebugLog
                            public void onConsumeFinished(Purchase purchase, IabResult result) {
                                pref.isPurchased = false;
                                pref.setEnabled(true);
                                pref.setSummary(R.string.settings_donate_unpurchased);
                            }
                        });
                    } else {
                        SkuDetails d = inv.getSkuDetails(sku);
                        if (d != null) {
                            Log.d(TAG, "unpurchased: " + d.toString());
                            addPreference(d);
                        }
                    }
                }
            } else {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.iab_unable_to_fetch_inventory)
                        .setNegativeButton(android.R.string.ok, null)
                        .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mIabHelper != null) {
                                    mIabHelper.queryInventoryAsync(true, IabUtil.PRODUCT_SKUS, mQueryListener);
                                }
                            }
                        })
                        .show();
            }
        }
    };

    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        @DebugLog
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (mIabHelper == null) {
                return;
            }
            if (result.isSuccess() && IabUtil.verifyDeveloperPayload(info)) {
                Log.d(TAG, "purchase success");
                String sku = info.getSku();
                PurchasePreference p = (PurchasePreference) mPrefscreen.findPreference(sku);
                if (p != null) {
                    p.isPurchased = true;
                    p.setSummary(R.string.iab_purchased);
                }
                switch (sku) {
                    case IabUtil.SKU_DONATE_ONE:
                        break;
                    case IabUtil.SKU_DONATE_TWO:
                        break;
                    case IabUtil.SKU_DONATE_THREE:
                        break;
                }
            } else {
                Log.d(TAG, "purchase failed");
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.iab_purchase_failed)
                        .setMessage(result.getMessage())
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }
    };

    private static class PurchasePreference extends Preference {
        private boolean isPurchased;
        private SkuDetails skuDetails;
        public PurchasePreference(Context context, SkuDetails details) {
            super(context);
            this.skuDetails = details;
            setKey(details.getSku());
            setTitle(details.getTitle());
            setSummary(R.string.settings_donate_unpurchased);
        }
    }
}
