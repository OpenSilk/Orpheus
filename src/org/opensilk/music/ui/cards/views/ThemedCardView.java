package org.opensilk.music.ui.cards.views;

import android.content.Context;
import android.util.AttributeSet;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.music.ui.cards.AbsBasicCard;

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
    protected void retrieveLayoutIDs() {
        super.retrieveLayoutIDs();
        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            changeBackgroundResourceId(R.drawable.card_background_light);
        } else {
            changeBackgroundResourceId(R.drawable.card_background_dark);
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

}
