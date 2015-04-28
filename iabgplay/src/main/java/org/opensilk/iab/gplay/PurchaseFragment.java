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

import android.support.v4.app.Fragment;

import org.opensilk.iab.gplay.util.IabHelper;
import org.opensilk.iab.gplay.util.IabResult;
import org.opensilk.iab.gplay.util.Purchase;

/**
 * If user rotates device while in the purchase dialog the DonateFragment becomes detached
 * and an new one takes its place, So can't keep the callback in that fragment
 * this Fragment is a proxy for the callback to allow detach/reattach.
 *
 * Created by drew on 4/22/15.
 */
public class PurchaseFragment extends Fragment implements IabHelper.OnIabPurchaseFinishedListener {
    public interface PurchaseCallback {
        void onResult(IabResult result, Purchase info);
    }

    static class Holder {
        final IabResult result;
        final Purchase info;

        public Holder(IabResult result, Purchase info) {
            this.result = result;
            this.info = info;
        }
    }

    PurchaseCallback mListener;
    Holder mHolder;

    public PurchaseFragment() {
        setRetainInstance(true);
    }

    public void setListener(PurchaseCallback l) {
        mListener = l;
        if (l != null && mHolder != null) {
            l.onResult(mHolder.result, mHolder.info);
        }
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
        if (mListener != null) {
            mListener.onResult(result, info);
        } else {
            mHolder = new Holder(result, info);
        }
    }
}
