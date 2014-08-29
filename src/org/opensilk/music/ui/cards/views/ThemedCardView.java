package org.opensilk.music.ui.cards.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.util.AttributeSet;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.ui.cards.AbsBasicCard;
import org.opensilk.music.util.PaletteUtil;

import butterknife.ButterKnife;
import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by drew on 3/17/14.
 */
public class ThemedCardView extends CardView implements Palette.PaletteAsyncListener {

    private static final int DESC_OVERLAY_ALPHA = 0xcc;
    protected int mDescOverlayDefaultColor;
    protected View mDescOverlay;

    public ThemedCardView(Context context) {
        super(context);
        getOverlayColor(context);
    }

    public ThemedCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getOverlayColor(context);
    }

    public ThemedCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getOverlayColor(context);
    }

    void getOverlayColor(Context context) {
        mDescOverlayDefaultColor = ThemeHelper.setColorAlpha(ThemeHelper.getAccentColor(context), DESC_OVERLAY_ALPHA);
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            changeBackgroundResourceId(R.drawable.card_background_light);
        } else {
            changeBackgroundResourceId(R.drawable.card_background_dark);
        }
        mDescOverlay = findViewById(R.id.griditem_desc_overlay);
        if (mDescOverlay != null) {
            //reset color for recycle
            setDescOverlayBackground(mDescOverlayDefaultColor, false);
            ArtworkImageView img = ButterKnife.findById(this, R.id.artwork_thumb);
            if (img != null) {
                img.installListener(this);
            }
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        if (mDescOverlay != null) {
            PaletteItem item = PaletteUtil.getBackgroundItem(palette);
            if (item != null) {
                final int backgroundColor = ThemeHelper.setColorAlpha(item.getRgb(), DESC_OVERLAY_ALPHA);
                if (backgroundColor != mDescOverlayDefaultColor) {
                    setDescOverlayBackground(backgroundColor, true);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCard != null && (mCard instanceof AbsBasicCard<?>)) {
            ((AbsBasicCard<?>) mCard).onViewDetachedFromWindow();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setRecycle(boolean isRecycle) {
        super.setRecycle(isRecycle);
        if (isRecycle && mCard != null && (mCard instanceof AbsBasicCard<?>)) {
            ((AbsBasicCard<?>) mCard).onViewRecycled();
        }
    }

    protected void setDescOverlayBackground(int color, boolean animate) {
        if (mDescOverlay != null) {
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
}
