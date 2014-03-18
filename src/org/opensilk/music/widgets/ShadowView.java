package org.opensilk.music.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/17/14.
 */
public class ShadowView extends ImageView {

    private static final int UP = 0;
    private static final int DOWN = 1;

    public ShadowView(Context context) {
        this(context, null);
    }

    public ShadowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int direction = UP;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ShadowView);
            if (a != null) {
                direction = a.getInt(R.styleable.ShadowView_shadowPosition, UP);
                a.recycle();
            }
        }

        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            setImageResource((direction == UP) ? R.drawable.above_shadow : R.drawable.below_shadow);
        } else {
            setImageResource((direction == UP) ? R.drawable.above_shadow_light : R.drawable.below_shadow_light);
        }
    }
}
