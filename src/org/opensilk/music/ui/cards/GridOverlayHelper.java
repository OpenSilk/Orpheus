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

package org.opensilk.music.ui.cards;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.view.View;

import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.util.PaletteUtil;

/**
 * Created by drew on 8/30/14.
 */
public class GridOverlayHelper implements Palette.PaletteAsyncListener {

    private static final int DESC_OVERLAY_ALPHA = 0xcc;
    private int mDescOverlayDefaultColor;

    private final View mDescOverlay;

    public GridOverlayHelper(View overlay) {
        mDescOverlay = overlay;
    }

    public static GridOverlayHelper create(Context context, View overlay) {
        return new GridOverlayHelper(overlay).init(context);
    }

    public GridOverlayHelper init(Context context) {
        mDescOverlayDefaultColor = ThemeHelper.setColorAlpha(ThemeHelper.getAccentColor(context), DESC_OVERLAY_ALPHA);
        setDescOverlayBackground(mDescOverlayDefaultColor, false);
        return this;
    }

    @Override
    public void onGenerated(Palette palette) {
        Palette.Swatch item = PaletteUtil.getBackgroundItem(palette);
        if (item != null) {
            final int backgroundColor = ThemeHelper.setColorAlpha(item.getRgb(), DESC_OVERLAY_ALPHA);
            if (backgroundColor != mDescOverlayDefaultColor) {
                setDescOverlayBackground(backgroundColor, true);
            }
        }
    }

    protected void setDescOverlayBackground(int color, boolean animate) {
        if (animate) {
            final TransitionDrawable overlayBackground = new TransitionDrawable(new Drawable[] {
                    new ColorDrawable(mDescOverlayDefaultColor),
                    new ColorDrawable(color),
            });
            overlayBackground.startTransition(200);
            mDescOverlay.setBackgroundDrawable(overlayBackground);
        } else {
            mDescOverlay.setBackgroundColor(color);
        }
    }

}
