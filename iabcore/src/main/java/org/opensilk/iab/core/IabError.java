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

package org.opensilk.iab.core;

/**
 * Created by drew on 4/21/15.
 */
public class IabError extends Exception {
    public enum Kind {
        NO_PROVIDER,
        QUERY_FAILED,
        NO_SKUS,
        PURCHASE_FAILED,
    }

    final Kind kind;

    IabError(Kind kind, String detailMessage) {
        super(detailMessage);
        this.kind = kind;
    }

    public static IabError noProvider(String msg) {
        return new IabError(Kind.NO_PROVIDER, msg);
    }

    public static IabError queryFailed(String msg) {
        return new IabError(Kind.QUERY_FAILED, msg);
    }

    public static IabError noSkus(String msg) {
        return new IabError(Kind.NO_SKUS, msg);
    }

    public static IabError purchaseFailed(String msg) {
        return new IabError(Kind.PURCHASE_FAILED, msg);
    }

    @Override
    public String toString() {
        return "IabError{ " + kind + "(" + getMessage() + ") }";
    }
}
