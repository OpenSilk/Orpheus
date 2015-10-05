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

package org.opensilk.music.artwork;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import static org.opensilk.music.artwork.Constants.*;

/**
 * Created by drew on 3/29/14.
 */
public enum ArtworkType {
    THUMBNAIL,
    LARGE;

    public int px = -1;

    //TODO this in only used an ArtwokrRequst2, move it in there.
    public static int getWidth(Context context, ArtworkType type) {
        switch (type) {
            case LARGE:
                if (LARGE.px < 0) {
                    LARGE.px = Math.min(getMinDisplayWidth(context), convertDpToPx(context, MAX_ARTWORK_SIZE_DP));
                }
                return LARGE.px;
            case THUMBNAIL:
            default:
                if (THUMBNAIL.px < 0) {
                    THUMBNAIL.px = convertDpToPx(context, DEFAULT_THUMBNAIL_SIZE_DP);
                }
                return THUMBNAIL.px;
        }
    }

    public static ArtworkType opposite(ArtworkType artworkType) {
        return artworkType == THUMBNAIL ? LARGE : THUMBNAIL;
    }

    /** Converts given dp value to density specific pixel value */
    public static int convertDpToPx(Context context, float dp) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    /** Returns smallest screen dimension */
    public static int getMinDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.min(metrics.widthPixels, metrics.heightPixels);
    }
}
