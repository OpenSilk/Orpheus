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

package org.opensilk.iab.gplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.opensilk.common.content.WrappedAsyncTaskLoader;
import org.opensilk.common.dagger.DaggerInjector;
import org.opensilk.iab.gplay.util.IabException;
import org.opensilk.iab.gplay.util.IabHelper;
import org.opensilk.iab.gplay.util.IabResult;
import org.opensilk.iab.gplay.util.Inventory;
import org.opensilk.iab.gplay.util.Purchase;
import org.opensilk.iab.gplay.util.SkuDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static org.opensilk.iab.core.DonateManager.D;
import static org.opensilk.iab.core.DonateManager.T;

/**
 * Created by drew on 4/26/14.
 */
public class DonateFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<DonateFragment.Item>>,
        PurchaseFragment.PurchaseCallback {
    static final String TAG = DonateFragment.class.getSimpleName();

    AlertDialog mPurchaseDialog;
    ItemAdapter mAdapter;
    PurchaseFragment mPurchaseFragment;

    @Inject IabHelper mIabHelper;

    static DonateFragment newInstance(boolean ok) {
        DonateFragment f = new DonateFragment();
        Bundle b = new Bundle();
        b.putBoolean("OK", ok);
        f.setArguments(b);
        return f;
    }

    public DonateFragment() {
        if (D) Log.d(TAG, "New instance");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DaggerInjector) activity).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ItemAdapter(getActivity());
        mPurchaseFragment = (PurchaseFragment) getActivity()
                .getSupportFragmentManager().findFragmentByTag("purchase");
        mPurchaseFragment.setListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
        mPurchaseFragment.setListener(null);
        mPurchaseFragment = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.iab_opensilk_donate));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissPurchaseDialog();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments().getBoolean("OK")) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Item item = mAdapter.getItem(position);
        if (item.purchased) {
            if (T) { //XXX for testing only
                if (D) Log.d(TAG, "consuming " + item.deets.getSku());
                try {
                    Inventory inventory = mIabHelper.queryInventory(false, null);
                    mIabHelper.consumeAsync(inventory.getPurchase(item.deets.getSku()),
                            new IabHelper.OnConsumeFinishedListener() {
                                @Override
                                public void onConsumeFinished(Purchase purchase, IabResult result) {
                                    getLoaderManager().restartLoader(0, null, DonateFragment.this);
                                }
                            }
                    );
                } catch (IabException e) {
                    Log.e(TAG, "Unablet to consume item", e);
                }
            } else {
                Log.w(TAG, "Whoa already purchased");
            }
            return;
        }
        showPurchaseDialog(item.deets);
    }

    @Override
    public Loader<List<Item>> onCreateLoader(int id, Bundle args) {
        return new ItemLoader(getActivity(), mIabHelper);
    }

    @Override
    public void onLoadFinished(Loader<List<Item>> loader, List<Item> data) {
        mAdapter.clear();
        mAdapter.addAll(data);
        setListAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<Item>> loader) {
    }

    @Override
    public void onResult(IabResult result, Purchase info) {
        if (result.isSuccess() && DonateProviderImpl.verifyDeveloperPayload(info)) {
            if (D) Log.d(TAG, "purchase success");
            getLoaderManager().restartLoader(0, null, DonateFragment.this);
            Toast.makeText(getActivity(), R.string.iab_msg_thanks, Toast.LENGTH_LONG).show();
        } else {
            if (D) Log.d(TAG, "purchase failed");
            showErrorDialog(getString(R.string.iab_err_purchase_failed), result.getMessage());
        }
    }

    private void showPurchaseDialog(final SkuDetails details) {
        mPurchaseDialog = new AlertDialog.Builder(getActivity())
                .setTitle(details.getTitle())
                .setMessage(details.getDescription())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIabHelper.launchPurchaseFlow(getActivity(), details.getSku(),
                                DonateActivity.PURCHASE_REQ,
                                mPurchaseFragment, DonateProviderImpl.developerPayload);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    void dismissPurchaseDialog() {
        if (mPurchaseDialog != null && mPurchaseDialog.isShowing()) {
            mPurchaseDialog.dismiss();
        }
    }

    void showErrorDialog(String title, String message) {
        mPurchaseDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
//                .setPositiveButton(R.string.iab_retry, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (mIabHelper != null) {
//                            mIabHelper.queryInventoryAsync(true, DonateProviderImpl.PRODUCT_SKUS, mQueryListener);
//                        }
//                    }
//                })
                .show();
    }

    static class ItemLoader extends WrappedAsyncTaskLoader<List<Item>> {
        final IabHelper helper;

        public ItemLoader(Context context, IabHelper helper) {
            super(context);
            this.helper =helper;
        }

        @Override
        public List<Item> loadInBackground() {
            try {
                Inventory inv = helper.queryInventory(true, DonateProviderImpl.PRODUCT_SKUS);
                if (inv.getSkuCount() == 0) {
                    return Collections.emptyList();
                }
                ArrayList<Item> items = new ArrayList<>(inv.getSkuCount());
                for (String sku : DonateProviderImpl.PRODUCT_SKUS) {
                    if (inv.hasDetails(sku)) {
                        if (D) Log.d(TAG, "found sku " + sku);
                        Item item = new Item(inv.getSkuDetails(sku));
                        if (inv.hasPurchase(sku) &&
                                DonateProviderImpl.verifyDeveloperPayload(inv.getPurchase(sku))) {
                            if (D) Log.d(TAG, "verified purchase for " + sku);
                            item.setPurchased(true);
                        }
                        items.add(item);
                    }
                }
                return items;
            } catch (IabException e) {
                return Collections.emptyList();
            }
        }
    }

    static class ItemAdapter extends ArrayAdapter<Item> {
        public ItemAdapter(Context context) {
            super(context, -1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            TextView text1;
            TextView text2;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            } else {
                view = convertView;
            }

            text1 = (TextView) view.findViewById(android.R.id.text1);
            text2 = (TextView) view.findViewById(android.R.id.text2);

            Item item = getItem(position);

            text1.setText(item.deets.getTitle() + " (" + item.deets.getPrice() + ")");
            text2.setText(item.purchased ? R.string.iab_purchased : R.string.iab_donate_unpurchased);

            return view;
        }
    }

    static class Item {
        final SkuDetails deets;
        boolean purchased;

        public Item(SkuDetails deets) {
            this.deets = deets;
        }

        public void setPurchased(boolean yes) {
            purchased = yes;
        }
    }

}
