package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by drew on 3/17/14.
 */
public class ThemedCardView extends CardView {

    public ThemedCardView(Context context) {
        super(context);
    }

    public ThemedCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
    }
}
