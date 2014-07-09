package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.util.AttributeSet;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.artwork.ArtworkImageView;

import butterknife.ButterKnife;
import it.gmariotti.cardslib.library.view.CardView;
import timber.log.Timber;

/**
 * Created by drew on 3/17/14.
 */
public class ThemedCardView extends CardView implements Palette.PaletteAsyncListener {

    protected int defaultOverlayColor;
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
        defaultOverlayColor = ThemeHelper.setColorAlpha(ThemeHelper.getAccentColor(context), 0xcc);
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
            mDescOverlay.setBackgroundColor(defaultOverlayColor);
            ArtworkImageView img = ButterKnife.findById(this, R.id.artwork_thumb);
            if (img != null) {
                img.installListener(this);
            }
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        if (mDescOverlay != null) {
            PaletteItem item = palette.getVibrantColor();
            if (item == null) {
                Timber.w("Trying muted palette");
                item = palette.getMutedColor();
            }
            if (item == null) {
                Timber.w("Unable to get palette");
            }
            if (item != null) {
                // todo animate
                mDescOverlay.setBackgroundColor(ThemeHelper.setColorAlpha(item.getRgb(), 0xcc));
            }
        }
    }
    /*
    new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                View parent = (View) getParent();
                if (parent != null) {
                    View overlay = parent.findViewById(R.id.griditem_desc_overlay);
                    if (overlay != null) {
                        PaletteItem item = palette.getDarkVibrantColor();
                        if (item == null) {
                            item = palette.getVibrantColor();
                        }
                        if (item != null) {
                            overlay.setBackgroundColor(item.getRgb());
                        }
                    }
                }
            }
        }
     */
}
