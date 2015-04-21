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
package org.opensilk.music.dream.mover;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.os.Handler;
import android.util.Log;
import android.view.View;

public class DayDreamAnimation implements Runnable {

    static final long SLIDE_TIME = 9000;
    static final long FADE_TIME = 3000;
    static final long DELAY = 3000;
    static final long ERR_DELAY = 1000;

    static final TimeInterpolator sSlowStartWithBrakes = new TimeInterpolator() {
        @Override
        public float getInterpolation(float x) {
            return (float)(Math.cos((Math.pow(x,3) + 1) * Math.PI) / 2.0f) + 0.5f;
        }
    };

    static enum Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    View mContentView, mSaverView;
    final Handler mHandler;
    Edge mNextEdge = Edge.RIGHT;

    public DayDreamAnimation(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public void registerViews(View contentView, View saverView) {
        mContentView = contentView;
        mSaverView = saverView;
    }

    @Override
    public void run() {
        if (mContentView == null || mSaverView == null) {
            rePost(ERR_DELAY);
            return;
        }

        final float xrange = mContentView.getWidth() - mSaverView.getWidth();
        final float yrange = mContentView.getHeight() - mSaverView.getHeight();
        Log.v("DayDreamAnimation", "xrange: " + xrange + " yrange: " + yrange);

        if (xrange == 0 && yrange == 0) {
            rePost(ERR_DELAY);
            return;
        }

        final int nextx = getNextX(xrange);
        final int nexty = getNextY(yrange);
        Log.v("DayDreamAnimation", "x: " + nextx + " y: " + nexty);

        updateEdge();

        if (mSaverView.getAlpha() == 0f) {
            // jump right there
            mSaverView.setX(nextx);
            mSaverView.setY(nexty);
            ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f)
                    .setDuration(FADE_TIME)
                    .start();
            rePost(FADE_TIME);
            return;
        }

        AnimatorSet s = new AnimatorSet();
        Animator xMove = ObjectAnimator.ofFloat(mSaverView, "x", mSaverView.getX(), nextx);
        Animator yMove = ObjectAnimator.ofFloat(mSaverView, "y", mSaverView.getY(), nexty);

        s.play(xMove).with(yMove);
        s.setDuration(SLIDE_TIME);
        s.setInterpolator(sSlowStartWithBrakes);
        s.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                rePost(DELAY);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mHandler.removeCallbacks(DayDreamAnimation.this);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        s.start();
    }

    void rePost(long delay) {
        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, delay);
    }

    int getNextX(float x) {
        switch (mNextEdge) {
            case LEFT:
                return 0;
            case RIGHT:
                return Math.round(x);
            case TOP:
            case BOTTOM:
            default:
                return Math.round((float) Math.random() * x);
        }
    }

    int getNextY(float y) {
        switch (mNextEdge) {
            case TOP:
                return 0;
            case BOTTOM:
                return Math.round(y);
            case LEFT:
            case RIGHT:
            default:
                return Math.round((float) Math.random() * y);
        }
    }

    void updateEdge() {
        switch (mNextEdge) {
            case LEFT:
                mNextEdge = Edge.TOP;
                break;
            case RIGHT:
                mNextEdge = Edge.BOTTOM;
                break;
            case TOP:
                mNextEdge = Edge.RIGHT;
                break;
            case BOTTOM:
                mNextEdge = Edge.LEFT;
                break;
        }
    }

}
