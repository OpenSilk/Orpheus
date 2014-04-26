package org.opensilk.music.iab;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;

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

    public static final String SKU_DONATE_ONE = "android.test.purchased";// "early_adopter";
    public static final String SKU_DONATE_TWO = "android.test.canceled"; // "donate_one";
    public static final String SKU_DONATE_THREE = "donate_three";

    public static final List<String> PRODUCT_SKUS;
    static {
        PRODUCT_SKUS = new ArrayList<>();
        PRODUCT_SKUS.add(SKU_DONATE_ONE);
        PRODUCT_SKUS.add(SKU_DONATE_TWO);
        PRODUCT_SKUS.add(SKU_DONATE_THREE);
    }

    public static IabHelper newHelper(Context context) {
        return new IabHelper(context, base64EncodedPublicKey);
    }

    public static final String PREF_APP_LAUNCHES = "app_launches";
    public static final String PREF_NEXT_BOTHER = "iab_next_bother";
    public static void incrementAppLaunchCount(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        int prevCount = p.getInt(PREF_APP_LAUNCHES, 0);
        p.edit().putInt(PREF_APP_LAUNCHES, ++prevCount).apply();
    }

    private static final int MIN_LAUNCHES = 5;
    private static final long THREE_WEEKS_MILLI = 3 * 7 * 24 * 60 * 60 * 1000;
    public static void maybeShowDonateDialog(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        // feeble attempt to not annoy early adopters with the popup,
        // pref is only present in versions <= 0.4.3
//        if (p.getBoolean("old_cache_removed", false)) {
//            return;
//        }
//        long nextBother = p.getLong(PREF_NEXT_BOTHER, 0);
//        int openCount = p.getInt(PREF_APP_LAUNCHES, 0);
//        if (openCount >= MIN_LAUNCHES && nextBother <= System.currentTimeMillis()) {
//            p.edit().putInt(PREF_APP_LAUNCHES, 0)
//                    .putLong(PREF_NEXT_BOTHER, System.currentTimeMillis() + THREE_WEEKS_MILLI)
//                    .apply();
            showDonateDialog(context);
//        }
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
        final IabHelper helper = new IabHelper(context, base64EncodedPublicKey);
        helper.enableDebugLogging(BuildConfig.DEBUG);
        helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    helper.queryInventoryAsync(false, PRODUCT_SKUS, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                Log.d(TAG, "result success");
                                Purchase p = inv.getPurchase(SKU_DONATE_ONE);
                                if (p != null && verifyDeveloperPayload(p)) {
                                    Log.d(TAG, "earlyAdopter:" + p.toString());
                                    EventBus.getInstance().post(new IABQueryResult(IABQueryResult.QError.NO_ERROR, true));
                                    return;
                                }
                                p = inv.getPurchase(SKU_DONATE_TWO);
                                if (p != null && verifyDeveloperPayload(p)) {
                                    Log.d(TAG, "donateOne:" + p.toString());
                                    EventBus.getInstance().post(new IABQueryResult(IABQueryResult.QError.NO_ERROR, true));
                                    return;
                                }
                                Log.d(TAG, "User has no purchases");
                                EventBus.getInstance().post(new IABQueryResult(IABQueryResult.QError.NO_ERROR, false));
                            } else {
                                EventBus.getInstance().post(new IABQueryResult(IABQueryResult.QError.QUERY_FAILED, false));
                            }
                            helper.dispose();
                        }
                    });
                } else {
                    EventBus.getInstance().post(new IABQueryResult(IABQueryResult.QError.BIND_FAILED, false));
                    helper.dispose();
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
