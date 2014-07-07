package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by drew on 3/17/14.
 */
public class ThemedCardView extends CardView {

    protected int defaultOverlayColor;

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
        defaultOverlayColor = ThemeHelper.getAccentColor(context);
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
        View overlay = findViewById(R.id.griditem_desc_overlay);
        if (overlay != null) {
            overlay.setBackgroundColor(defaultOverlayColor);
        }
    }
}
