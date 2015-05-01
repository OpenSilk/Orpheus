/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by drew on 10/14/14.
 */
public abstract class FlingyFabLayout extends RelativeLayout {

    protected enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

    final ViewDragHelper dragHelper;

    int verticalRange;
    int horizontalRange;
    int draggingState;

    public FlingyFabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        dragHelper = ViewDragHelper.create(this, new DragHelperCallback());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragHelper.shouldInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        dragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        verticalRange = h;
        horizontalRange = w;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Called when child is release, override to perform actions
     */
    protected void onFabFling(View child, Direction direction) {

    }

    public abstract boolean canCaptureView(View child, int pointerId);

    private class DragHelperCallback extends ViewDragHelper.Callback {

        int startPosTop;
        int startPosLeft;

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == draggingState) return;
            draggingState = state;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            startPosTop = capturedChild.getTop();
            startPosLeft = capturedChild.getLeft();
//            Timber.d("onViewCaptured startPos x=%d, y=%d", startPosTop, startPosLeft);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
//            Timber.d( "onViewReleased(xvel=%f, yvel=%f", xvel, yvel);
            int endPosTop = releasedChild.getTop();
            int entPosLeft = releasedChild.getLeft();
            int dTop = startPosTop - endPosTop;
            int dLeft = startPosLeft - entPosLeft;
//            Timber.d( "Direction dTop=%d dLeft=%d", dTop, dLeft);
            String dir = "";
            if (Math.abs(dTop) > Math.abs(dLeft)) {
                //VERTICAL
                dir += "VERTICAL";
                if (dTop > 0) {
                    //UP
                    dir += " UP";
                    onFabFling(releasedChild, Direction.UP);
                } else {
                    //DOWN
                    dir += " DOWN";
                    onFabFling(releasedChild, Direction.DOWN);
                }
            } else {
                //HORIZONTAL
                dir += "HORIZONTAL";
                if (dLeft > 0) {
                    //LEFT
                    dir += " LEFT";
                    onFabFling(releasedChild, Direction.LEFT);
                } else {
                    //RIGHT
                    dir += " RIGHT";
                    onFabFling(releasedChild, Direction.RIGHT);
                }
            }
//            Timber.d( "Overall Direction = %s", dir);
            dragHelper.settleCapturedViewAt(startPosLeft, startPosTop);
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return horizontalRange;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return verticalRange;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return canCaptureView(child, pointerId);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }
    }

}
