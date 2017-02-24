/*
 * Copyright (c) 2011, olberg(at)gmail(dot)com,
 *                     http://mobile-anarchy-widgets.googlecode.com
 * Copyright (c) 2011, Joakim Andersson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * # Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * # Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * As can be seen in the copyright above, this class is a modified version of
 * the JoystickView class available from the mobile-anarchy-widgets project on
 * Google Code. I've stripped the code to my needs and added some customization
 * options that I needed for my own project.
 *
 * 		/Joakim Andersson, 2011-11-11
 */

/****************************************************************************
 * Copyright (c) 2016 Ramakrishna Kintada. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name ATLFlight nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * In addition Supplemental Terms apply.  See the SUPPLEMENTAL file.
 ****************************************************************************/

package atlflight.dronecontroller.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class SliderView extends View {
    public static final int INVALID_POINTER_ID = -1;

    // =========================================
    // Private Members
    // =========================================
    private final boolean D = false;
    String TAG = "SliderView";

    private Paint paintThumbstick;
    private Paint paintBackground;

    private int innerPadding;
    private int radiusBackground;
    private int radiusThumbstick;
    private int radiusMovement;

    private JoystickMovedListener moveListener;
    private JoystickClickedListener clickListener;

    //# of pixels movement required between reporting to the listener
    private float moveResolution;

    private boolean axisInverted;
    private boolean autoReturnToCenter;

    //Max range of movement in user coordinate system
    public final static int CONSTRAIN_BOX = 0;
    public final static int CONSTRAIN_CIRCLE = 1;
    private int movementConstraint;
    private float movementRange;

    //Records touch pressure for click handling
    private float touchPressure;
    private boolean clicked;
    private float clickThreshold;

    //Last touch point in view coordinates
    private int pointerId = INVALID_POINTER_ID;
    private float touchX, touchY;

    //Last reported position in view coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    //Center of the view in view coordinates
    private int cX, cY;

    //Size of the view in view coordinates
    private int dimX, dimY;

    //Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private int offsetX;
    private int offsetY;

    private float initialXPosAdj = 0;
    private float initialYPosAdj = 0;

    // =========================================
    // Constructors
    // =========================================

    public SliderView(Context context) {
        super(context);
        initJoystickView();
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();
    }

    public SliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initJoystickView();
    }

    // =========================================
    // Initialization
    // =========================================

    private void initJoystickView() {
        setFocusable(true);

        Paint dbgPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        dbgPaint1.setColor(Color.RED);
        dbgPaint1.setStrokeWidth(1);
        dbgPaint1.setStyle(Paint.Style.STROKE);

        Paint dbgPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        dbgPaint2.setColor(Color.GREEN);
        dbgPaint2.setStrokeWidth(1);
        dbgPaint2.setStyle(Paint.Style.STROKE);

        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setColor(Color.GRAY);
        paintBackground.setStrokeWidth(1);
        paintBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        paintThumbstick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintThumbstick.setColor(Color.DKGRAY);
        paintThumbstick.setStrokeWidth(1);
        paintThumbstick.setStyle(Paint.Style.FILL_AND_STROKE);

        innerPadding = 10;

        setMovementRange(150);
        setMoveResolution(1.0f);
        setClickThreshold(0.4f);
        setAutoReturnToCenter(true);
        setVisibility(View.VISIBLE);
    }

    public void setAutoReturnToCenter(boolean xAxisAutoReturnToCenter) {
        this.autoReturnToCenter = xAxisAutoReturnToCenter;
    }

    public boolean isXAxisAutoReturnToCenter() {
        return autoReturnToCenter;
    }

    public void setMovementConstraint(int movementConstraint) {
        if (movementConstraint < CONSTRAIN_BOX || movementConstraint > CONSTRAIN_CIRCLE)
            Log.e(TAG, "invalid value for movementConstraint");
        else
            this.movementConstraint = movementConstraint;
    }

    public int getMovementConstraint() {
        return movementConstraint;
    }

    public boolean isXAxisInverted() {
        return axisInverted;
    }


    public void setXAxisInverted(boolean xAxisInverted) {
        this.axisInverted = xAxisInverted;
    }

    /**
     * Set the pressure sensitivity for registering a click
     *
     * @param clickThreshold threshold 0...1.0f inclusive. 0 will cause clicks to never be reported, 1.0 is a very hard click
     */
    public void setClickThreshold(float clickThreshold) {
        if (clickThreshold < 0 || clickThreshold > 1.0f)
            Log.e(TAG, "clickThreshold must range from 0...1.0f inclusive");
        else
            this.clickThreshold = clickThreshold;
    }

    public float getClickThreshold() {
        return clickThreshold;
    }

    public void setMovementRange(float movementRange) {
        this.movementRange = movementRange;
    }

    public float getMovementRange() {
        return movementRange;
    }

    public void setMoveResolution(float moveResolution) {
        this.moveResolution = moveResolution;
    }

    public float getMoveResolution() {
        return moveResolution;
    }

    // =========================================
    // Public Methods
    // =========================================

    public void setOnJoystickMovedListener(JoystickMovedListener listener) {
        this.moveListener = listener;
    }

    public void setOnJoystickClickedListener(JoystickClickedListener listener) {
        this.clickListener = listener;
    }

    public void setPosition(float initPosAdj) {
        initialXPosAdj = initPosAdj;
    }


    // =========================================
    // Drawing Functionality
    // =========================================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Here we make sure that we have a perfect circle
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int d = Math.min(getMeasuredWidth(), getMeasuredHeight());

        dimX = d;
        dimY = d;

        cX = d / 2;
        cY = d / 2;

        radiusBackground = dimX / 2 - innerPadding;
        radiusThumbstick = (int) (d * 0.15);
        int handleInnerBoundaries = radiusThumbstick;
        radiusMovement = Math.min(cX, cY) - handleInnerBoundaries;
    }

    private int measure(int measureSpec) {
        int result = 0;
        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        // Draw the background
        canvas.drawRect(cX - radiusBackground / 15, cY - radiusBackground, cX + radiusBackground / 15, cY + radiusBackground, paintBackground);


        if (initialXPosAdj != 0 || initialYPosAdj != 0) {
            if (!autoReturnToCenter) {
                touchX = radiusMovement * initialXPosAdj;
            }
            initialXPosAdj = 0;
            initialYPosAdj = 0;
            reportOnMoved();
        }

        final float xLen = radiusThumbstick;
        final float yLen = radiusThumbstick / 5;
        // Draw the handle
        float handleX = cX;
        float handleY = touchY + cY;
        //canvas.drawCircle(handleX, handleY, radiusThumbstick, paintThumbstick);
        //canvas.drawRect(handleX-xLen, handleY - yLen, handleX+xLen, handleY + yLen, paintThumbstick);
        canvas.drawCircle(handleX, handleY, radiusThumbstick * 3 / 4, paintThumbstick);

        canvas.restore();
    }

    // Constrain touch within a box
    private void constrainBox() {
        touchX = Math.max(Math.min(touchX, radiusMovement), -radiusMovement);
        touchY = Math.max(Math.min(touchY, radiusMovement), -radiusMovement);
    }

    // Constrain touch within a circle
    private void constrainCircle() {
        float diffX = touchX;
        float diffY = touchY;
        double radial = Math.sqrt((diffX * diffX) + (diffY * diffY));
        if (radial > radiusMovement) {
            touchX = (int) ((diffX / radial) * radiusMovement);
            touchY = (int) ((diffY / radial) * radiusMovement);
        }
    }

    public void setPointerId(int id) {
        this.pointerId = id;
    }

    public int getPointerId() {
        return pointerId;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                return processMoveEvent(ev);
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (pointerId != INVALID_POINTER_ID) {
//                              Log.d(TAG, "ACTION_UP");
                    returnHandleToCenter();
                    setPointerId(INVALID_POINTER_ID);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                if (pointerId != INVALID_POINTER_ID) {
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    if (pointerId == this.pointerId) {
//                                      Log.d(TAG, "ACTION_POINTER_UP: " + pointerId);
                        returnHandleToCenter();
                        setPointerId(INVALID_POINTER_ID);
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                if (pointerId == INVALID_POINTER_ID) {
                    int x = (int) ev.getX();
                    if (x >= offsetX && x < offsetX + dimX) {
                        setPointerId(ev.getPointerId(0));
//                                      Log.d(TAG, "ACTION_DOWN: " + getPointerId());
                        return true;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (pointerId == INVALID_POINTER_ID) {
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    int x = (int) ev.getX(pointerId);
                    if (x >= offsetX && x < offsetX + dimX) {
//                                      Log.d(TAG, "ACTION_POINTER_DOWN: " + pointerId);
                        setPointerId(pointerId);
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private boolean processMoveEvent(MotionEvent ev) {
        if (pointerId != INVALID_POINTER_ID) {
            final int pointerIndex = ev.findPointerIndex(pointerId);

            // Translate touch position to center of view
            float x = ev.getX(pointerIndex);
            touchX = x - cX - offsetX;
            float y = ev.getY(pointerIndex);
            touchY = y - cY - offsetY;

//              Log.d(TAG, String.format("ACTION_MOVE: (%03.0f, %03.0f) => (%03.0f, %03.0f)", x, y, touchX, touchY));

            reportOnMoved();
            invalidate();

            touchPressure = ev.getPressure(pointerIndex);
            reportOnPressure();

            return true;
        }
        return false;
    }

    private void reportOnMoved() {
        if (movementConstraint == CONSTRAIN_CIRCLE)
            constrainCircle();
        else
            constrainBox();

        if (moveListener != null) {
            boolean rx = Math.abs(touchX - reportX) >= moveResolution;
            boolean ry = Math.abs(touchY - reportY) >= moveResolution;
            if (rx || ry) {
                this.reportX = touchX;
                this.reportY = touchY;
                int userX = (int) (-touchX / radiusMovement * movementRange);
                int userY = (int) (-touchY / radiusMovement * movementRange);

//                              Log.d(TAG, String.format("moveListener.OnMoved(%d,%d)", (int)userX, (int)userY));
                moveListener.onMoved(userX, userY);
            }
        }
    }


    //Simple pressure click
    private void reportOnPressure() {
//              Log.d(TAG, String.format("touchPressure=%.2f", this.touchPressure));
        if (clickListener != null) {
            if (clicked && touchPressure < clickThreshold) {
                clickListener.OnReleased();
                this.clicked = false;
//                              Log.d(TAG, "reset click");
                invalidate();
            } else if (!clicked && touchPressure >= clickThreshold) {
                clicked = true;
                clickListener.OnClicked();
//                              Log.d(TAG, "click");
                invalidate();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
    }

    private void returnHandleToCenter() {
        if (autoReturnToCenter) {
            final int numFrames = 5;
            final double intervalX = (autoReturnToCenter ? (0 - touchX) / numFrames : 0);
            final double intervalY = (autoReturnToCenter ? (0 - touchY) / numFrames : 0);

            for (int frameNum = 0; frameNum < numFrames; frameNum++) {
                final int lastFrame = frameNum;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        touchX += intervalX;
                        touchY += intervalY;

                        reportOnMoved();
                        invalidate();

                        if (moveListener != null && lastFrame == numFrames - 1) {
                            moveListener.onReturnedToCenter();
                        }
                    }
                }, frameNum * 40);
            }

            if (moveListener != null) {
                moveListener.onReleased();
            }
        }
    }

    public void setTouchOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }
}
