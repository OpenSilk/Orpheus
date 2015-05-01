/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.ui.widget;

import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageButton;

import org.opensilk.common.ui.R;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.core.util.VersionUtils;

/**
 * Created by drew on 10/30/14.
 */
public class FloatingActionButton extends ImageButton {

    public interface OnDoubleClickListener {
        void onDoubleClick(View view);
    }

    public static final int COLOR_ACCENT = 0;
    public static final int COLOR_WHITE =1;
    public static final int COLOR_BLACK = 2;

    protected int mColor;
    //Used to create the state list for compat drawable
    //https://code.google.com/p/android/issues/detail?id=26251
    protected int mColorNormal;
    protected int mColorPressed;

    private OnDoubleClickListener mOnDoubleClickListener;
    private GestureDetectorCompat mGestureDetector;

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetectorCompat(getContext(), new GestureListener());
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton, defStyleAttr, 0);
        if (attr != null) {
            try {
                mColor = attr.getInt(R.styleable.FloatingActionButton_fabColor, COLOR_ACCENT);
                switch (mColor) {
                    case COLOR_BLACK:
                        mColorNormal = Color.BLACK;
                        break;
                    case COLOR_WHITE:
                        mColorNormal = Color.WHITE;
                        break;
                    case COLOR_ACCENT:
                    default:
                        mColorNormal = ThemeUtils.getColorAccent(getContext());
                }
                mColorPressed = ThemeUtils.getColorControlNormal(getContext());
            } finally {
                attr.recycle();
            }
        }
        init();
    }

    protected void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (mColor) {
                case COLOR_BLACK:
                    setBackgroundResource(R.drawable.fab_ripple_black);
                    break;
                case COLOR_WHITE:
                    setBackgroundResource(R.drawable.fab_ripple_white);
                    break;
                case COLOR_ACCENT:
                default:
                    setBackgroundResource(R.drawable.fab_ripple_accent);
                    break;
            }
            setStateListAnimator(
                    AnimatorInflater.loadStateListAnimator(getContext(), R.animator.fab_elevation)
            );
            setElevation(getResources().getDimension(R.dimen.fab_elevation));
//            setImageResource(mIcon);
            return;
        } else {
            setBackgroundCompat(createSelectableDrawable());
//            setImageResource(mIcon);
        }
    }

    /**
     * >= api 21
     */
    @SuppressWarnings("NewApi")
    protected RippleDrawable createRippleDrawable() {
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.getPaint().setColor(mColorNormal);
        return new RippleDrawable(createRippleStateList(), drawable, null);
    }

    protected ColorStateList createRippleStateList() {
        int[][] states = new int[1][];
        int[] colors = new int[1];
        states[0] = new int[0];
        colors[0] = mColorPressed;
        return new ColorStateList(states, colors);
    }

    /**
     * <= api 19
     */
    protected Drawable createSelectableDrawable() {

        ShapeDrawable drawableNormal = new ShapeDrawable(new OvalShape());
        drawableNormal.getPaint().setColor(mColorNormal);

        StateListDrawable stateDrawable = new StateListDrawable();

        ShapeDrawable drawableHighlight = new ShapeDrawable(new OvalShape());
        drawableHighlight.getPaint().setColor(mColorPressed);

        stateDrawable.addState(new int[]{android.R.attr.state_pressed}, drawableHighlight);
        stateDrawable.addState(new int[0], null);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {drawableNormal, stateDrawable});

        return layerDrawable;
    }

    @Override
    public void setImageLevel(int level) {
        if (getDrawable() != null && getDrawable().getLevel() == level) return;
        maybeDoCircularReveal();
        super.setImageLevel(level);
    }

    @Override
    public void setImageState(int[] state, boolean merge) {
        if (getDrawable() != null && getDrawable().getState() == state) return;
        maybeDoCircularReveal();
        super.setImageState(state, merge);
    }

    @SuppressWarnings("NewApi")
    protected void maybeDoCircularReveal() {
        if (true) return; //XXX STUBBED till i make it look better
        if (VersionUtils.hasLollipop()) {
            ViewAnimationUtils
                    .createCircularReveal(this, getWidth() / 2, getHeight() / 2, 0, getHeight() * 2)
                    .start();
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void setBackgroundCompat(Drawable drawable) {
        if (VersionUtils.hasJellyBean()) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }

    public void setOnDoubleClickListener(OnDoubleClickListener l) {
        if (!isClickable()) setClickable(true);
        mOnDoubleClickListener = l;
    }

    protected boolean performDoubleClick() {
        if (mOnDoubleClickListener != null) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnDoubleClickListener.onDoubleClick(this);
            return true;
        } else {
            return performClick();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                setPressed(false);
                break;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return isClickable();
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public void onLongPress(MotionEvent e) {
            performLongClick();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return isClickable() && performClick();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return isClickable() && performDoubleClick();
        }

    }
}
