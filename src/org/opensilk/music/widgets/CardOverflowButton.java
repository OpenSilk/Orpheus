package org.opensilk.music.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/21/14.
 */
public class CardOverflowButton extends ImageButton {

    private final int mOverflowDrawable;

    public CardOverflowButton(Context context) {
        this(context, null);
    }

    public CardOverflowButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardOverflowButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            mOverflowDrawable = R.drawable.ic_menu_overflow_card_holo_light;
        } else {
            mOverflowDrawable = R.drawable.ic_menu_overflow_card_holo_dark;
        }
        setImageResource(mOverflowDrawable);
    }
}
