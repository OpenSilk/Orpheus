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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.iab.core.IabError;
import org.opensilk.iab.core.DonateManager;
import org.opensilk.iab.core.DonateProvider;
import org.opensilk.iab.gplay.util.IabHelper;
import org.opensilk.iab.gplay.util.IabResult;
import org.opensilk.iab.gplay.util.Inventory;
import org.opensilk.iab.gplay.util.Purchase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;

import static org.opensilk.iab.core.DonateManager.D;

/**
 * Created by drew on 4/21/15.
 */
@Singleton
public class DonateProviderImpl implements DonateProvider {
    private static final String TAG = "IABGPlay";

    public static final String SKU_DONATE_ONE = "donate_one";
    public static final String SKU_DONATE_TWO = "donate_two";
    public static final String SKU_DONATE_THREE = "donate_three";

    public static final List<String> PRODUCT_SKUS;
    static {
        PRODUCT_SKUS = new ArrayList<>();
        PRODUCT_SKUS.add(SKU_DONATE_ONE);
        PRODUCT_SKUS.add(SKU_DONATE_TWO);
        PRODUCT_SKUS.add(SKU_DONATE_THREE);
    }

    private final Context appContext;

    @Inject
    public DonateProviderImpl(@ForApplication Context appContext) {
        this.appContext = appContext;
    }

    public IabHelper newHelper() {
        IabHelper h = new IabHelper(appContext, base64EncodedPublicKey);
        h.enableDebugLogging(D);
        return h;
    }

    public Observable<Boolean> hasDonated() {
        return Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(final Subscriber<? super Boolean> subscriber) {
                        final IabHelper helper = newHelper();
                        helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                            @Override
                            public void onIabSetupFinished(IabResult result) {
                                if (result.isSuccess()) {
                                    helper.queryInventoryAsync(true, PRODUCT_SKUS, new IabHelper.QueryInventoryFinishedListener() {
                                        @Override
                                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                            helper.dispose();
                                            if (result.isSuccess()) {
                                                if (D) Log.d(TAG, "result success");
                                                if (D) Log.d(TAG, "purchases=" + inv.getAllPurchases());
                                                if (D) Log.d(TAG, "skus=" + inv.getAllSkus());
                                                // Debug builds, and other unforseen situations will have zero iab items
                                                if (inv.getSkuCount() == 0) {
                                                    subscriber.onError(IabError.noSkus(""));
                                                    return;
                                                }
                                                for (String sku : PRODUCT_SKUS) {
                                                    Purchase p = inv.getPurchase(sku);
                                                    if (p != null && verifyDeveloperPayload(p)) {
                                                        if (D) Log.d(TAG, "purchase: " + p);
                                                        subscriber.onNext(true);
                                                        subscriber.onCompleted();
                                                        return;
                                                    }
                                                }
                                                if (D) Log.d(TAG, "User has no purchases");
                                                subscriber.onNext(false);
                                                subscriber.onCompleted();
                                            } else {
                                                subscriber.onError(IabError.queryFailed(result.toString()));
                                            }
                                        }
                                    });
                                } else {
                                    helper.dispose();
                                    subscriber.onError(IabError.noProvider(result.toString()));
                                }
                            }
                        });
                    }
                }
        );
    }

    @Override
    public void launchDonateActivity(Activity context) {
        context.startActivity(new Intent(context, DonateActivity.class));
    }

    /** Verifies the developer payload of a purchase. */
    public static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        // Not very safe but w/e
        return developerPayload.equals(payload);
    }

    public static final String developerPayload = "6doBJswf1IgsbWLppNYvap6cbF4Fw3mNz6Bzy2Z1DflLu";
    private static final String base64EncodedPublicKey;
    static {
        base64EncodedPublicKey =  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnWiUCdPLrTOVJSCeTvI47"
                + "pZavSyqp8e+tK4aQNqdUtPdTh/ok3AjlgOWGM6vD+B7MK83c1lwYkZgCS57v2eDc5"
                + "vhToJRpukkDALK30s4tpkCGjtjmnbmtrafcJ8D8VFmifCgk9oW5S9fg4e+c0+Qzox"
                + "yQ+Gf0pAK0iV/vCSkJCMWxYgpNWsVDKvHpboMmjdV+wuWZUGPug1JnjkK8iln+EQ8"
                + "ZQ2DRowaVagxTlgKaBSHtGECz1psQU8o/F6WNkWU4QsnxGbG3C+wzX8N2vR95nelo"
                + "6DndHF3B7fcAZs25ZkGNpGvD2s8Dy7ohqzL8YDpE9xUfDPg99VajAEnpmsdyQIDAQAB";
    }
}
