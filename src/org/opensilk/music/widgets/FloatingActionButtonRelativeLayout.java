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

package org.opensilk.music.widgets;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.opensilk.music.R;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 10/14/14.
 */
public class FloatingActionButtonRelativeLayout extends RelativeLayout {

    final ViewDragHelper dragHelper;

    int verticalRange;
    int horizontalRange;
    int draggingState;

    public FloatingActionButtonRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        dragHelper = ViewDragHelper.create(this, new DragHelperCallback());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragHelper.shouldInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

    protected enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

    protected void onFabFling(Direction direction) {

    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        int startPosTop;
        int startPosLeft;

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == draggingState) return;
            if ((draggingState == ViewDragHelper.STATE_DRAGGING || draggingState == ViewDragHelper.STATE_SETTLING)
                    && state == ViewDragHelper.STATE_IDLE) {
                // the view stopped from moving.

                //TODO return to start
            }
            if (state == ViewDragHelper.STATE_DRAGGING) {
//                onStartDragging();
            }
            draggingState = state;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            startPosTop = capturedChild.getTop();
            startPosLeft = capturedChild.getLeft();
            Timber.d("onViewCaptured startPos x=%d, y=%d", startPosTop, startPosLeft);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            Timber.d( "onViewReleased(xvel=%f, yvel=%f", xvel, yvel);
            int endPosTop = releasedChild.getTop();
            int entPosLeft = releasedChild.getLeft();
            int dTop = startPosTop - endPosTop;
            int dLeft = startPosLeft - entPosLeft;
            Timber.d( "Direction dTop=%d dLeft=%d", dTop, dLeft);
            String dir = "";
            if (Math.abs(dTop) > Math.abs(dLeft)) {
                //VERTICAL
                dir += "VERTICAL";
                if (dTop > 0) {
                    //UP
                    dir += " UP";
                    onFabFling(Direction.UP);
                } else {
                    //DOWN
                    dir += " DOWN";
                    onFabFling(Direction.DOWN);
                }
            } else {
                //HORIZONTAL
                dir += "HORIZONTAL";
                if (dLeft > 0) {
                    //LEFT
                    dir += " LEFT";
                    onFabFling(Direction.LEFT);
                } else {
                    //RIGHT
                    dir += " RIGHT";
                    onFabFling(Direction.RIGHT);
                }
            }
            Timber.d( "Overall Direction = %s", dir);
            dragHelper.settleCapturedViewAt(startPosLeft, startPosTop);
            invalidate();
        }

        @Override
        public int getOrderedChildIndex(int index) {
            return super.getOrderedChildIndex(index);
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
            return child.getId() == R.id.floating_action_button;
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
