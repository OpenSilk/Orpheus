package org.opensilk.music.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.ThemeHelper;

/**
 * Created by drew on 3/17/14.
 */
public class ProfileHeaderLayout extends LinearLayout {

    public ProfileHeaderLayout(Context context) {
        this(context, null);
    }

    public ProfileHeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProfileHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        boolean isLightTheme = ThemeHelper.isLightTheme(getContext());
        if (isLightTheme) {
            setBackgroundColor(getResources().getColor(R.color.app_background_light_transparent));
        } else {
            setBackgroundColor(getResources().getColor(R.color.app_background_dark_transparent));
        }
    }
}
