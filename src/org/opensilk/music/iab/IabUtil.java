package org.opensilk.music.iab;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.IABQueryResult;
import org.opensilk.music.ui.settings.SettingsPhoneActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drew on 4/26/14.
 */
public class IabUtil {
    private static final String TAG = IabUtil.class.getSimpleName();
    public static final boolean T = false; // for testing iab;
    public static final boolean D = false;

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

    public static final String PREF_APP_LAUNCHES = "app_launches";
    public static final String PREF_NEXT_BOTHER = "iab_next_bother";

    private static final long ONE_MINUTE_MILLI = 60 * 1000;
    private static final long ONE_HOUR_MILLI = 60 * ONE_MINUTE_MILLI;
    private static final long ONE_DAY_MILLI = 24 * ONE_HOUR_MILLI;
    private static final long ONE_WEEK_MILLI = 7 * ONE_DAY_MILLI;
    private static final long MIN_INTERVAL_FOR_BOTHER = 2 * ONE_WEEK_MILLI;
    private static final int MIN_LAUNCHES_FOR_BOTHER = 4;

    public static IabHelper newHelper(Context context) {
        IabHelper h = new IabHelper(context, base64EncodedPublicKey);
        h.enableDebugLogging(D);
        return h;
    }

    public static void incrementAppLaunchCount(Context context) {
        PreferenceUtils p = PreferenceUtils.getInstance(context);
        int prevCount = p.getInt(PREF_APP_LAUNCHES, 0);
        p.putInt(PREF_APP_LAUNCHES, ++prevCount);
    }

    public static void maybeShowDonateDialog(Context context) {
        if (T) {
            showDonateDialog(context);
            return;
        }
        PreferenceUtils p = PreferenceUtils.getInstance(context);
        // feeble attempt to not annoy early adopters with the popup,
        // pref is only present in versions <= 0.4.3
        if (p.getBoolean("old_cache_removed", false)) {
            return;
        }
        long nextBother = p.getLong(PREF_NEXT_BOTHER, 0);
        int openCount = p.getInt(PREF_APP_LAUNCHES, 0);
        if (openCount >= MIN_LAUNCHES_FOR_BOTHER && nextBother <= System.currentTimeMillis()) {
            p.putInt(PREF_APP_LAUNCHES, 0);
            p.putLong(PREF_NEXT_BOTHER, System.currentTimeMillis() + MIN_INTERVAL_FOR_BOTHER);
            showDonateDialog(context);
        }
    }

    public static void showDonateDialog(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.iab_donate)
                .setMessage(R.string.iab_donate_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(new Intent(context, SettingsPhoneActivity.class)
                                .setAction("open_donate"));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Checks if user has purchases
     * posts a {@link org.opensilk.music.bus.events.IABQueryResult}
     * to the global event bus with purchase status
     * @param context
     */
    public static void queryDonateAsync(Context context) {
        final IabHelper helper = newHelper(context);
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
                                if (D) Log.d(TAG, "purchases=" + inv.mPurchaseMap.toString());
                                if (D) Log.d(TAG, "skus=" + inv.mSkuMap.toString());
                                // Debug builds, and other unforseen situations will have zero iab items
                                if (inv.getSkuCount() == 0) {
                                    EventBus.getInstance().post(new IABQueryResult(IABQueryResult.Error.NO_SKUS));
                                    return;
                                }
                                for (String sku : PRODUCT_SKUS) {
                                    Purchase p = inv.getPurchase(sku);
                                    if (p != null && verifyDeveloperPayload(p)) {
                                        Log.d(TAG, "purchase: " + p.toString());
                                        EventBus.getInstance().post(new IABQueryResult(IABQueryResult.Error.NO_ERROR, true));
                                        return;
                                    }
                                }
                                if (D) Log.d(TAG, "User has no purchases");
                                EventBus.getInstance().post(new IABQueryResult(IABQueryResult.Error.NO_ERROR, false));
                            } else {
                                EventBus.getInstance().post(new IABQueryResult(IABQueryResult.Error.QUERY_FAILED));
                            }
                        }
                    });
                } else {
                    // No billing service
                    EventBus.getInstance().post(new IABQueryResult(IABQueryResult.Error.BIND_FAILED));
                }
            }
        });
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
