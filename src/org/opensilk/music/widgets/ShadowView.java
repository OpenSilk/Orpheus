package org.opensilk.music.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opensilk.music.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/17/14.
 */
public class ShadowView extends ImageView {

    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int RIGHT = 2;

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
        setImageResource(getShadowResource(direction, isLightTheme));
    }

    private static int getShadowResource(int direction, boolean isLightTheme) {
        switch (direction) {
            case UP:
                return isLightTheme ? R.drawable.shadow_above : R.drawable.shadow_above_light;
            case DOWN:
                return isLightTheme ? R.drawable.shadow_below : R.drawable.shadow_below_light;
            case RIGHT:
                return isLightTheme ? R.drawable.shadow_right : R.drawable.shadow_right_light;
        }
        return -1;
    }
}
