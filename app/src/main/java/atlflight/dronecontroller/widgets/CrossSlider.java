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
 *
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


public class CrossSlider extends View {
    public static final int INVALID_POINTER_ID = -1;

    // =========================================
    // Private Members
    // =========================================
    private final boolean D = false;
    static final String TAG = "CrossSlider";

    private Paint paintBackground;
    private Paint paintThumbstick;
    private Paint paintOuterCircle;
    private Paint paintBackgroundCircle;

    private float radiusBackground;
    private float radiusOutline;
    private int radiusThumbstick;
    private int movementRadius;

    private JoystickMovedListener moveListener;
    private JoystickClickedListener clickListener;

    //# of pixels movement required between reporting to the listener
    private float moveResolution;

    private boolean xAxisInverted;
    private boolean yAxisInverted;
    private boolean xAxisAutoReturnToCenter;
    private boolean yAxisAutoReturnToCenter;

    //Max range of movement in user coordinate system
    public final static int CONSTRAIN_BOX = 0;
    public final static int CONSTRAIN_CIRCLE = 1;
    private int movementConstraint;
    private float movementRange;

    public final static int COORDINATE_CARTESIAN = 0;               //Regular cartesian coordinates
    public final static int COORDINATE_DIFFERENTIAL = 1;    //Uses polar rotation of 45 degrees to calc differential drive paramaters
    private int userCoordinateSystem;

    //Records touch pressure for click handling
    private float touchPressure;
    private boolean clicked;
    private float clickThreshold;

    //Last touch point in view coordinates
    private int pointerId = INVALID_POINTER_ID;
    private float touchX, touchY;

    //Last reported position in view coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    //Handle center in view coordinates
    private float handleX, handleY;

    //Center of the view in view coordinates
    private int cX, cY;

    //Size of the view in view coordinates
    private int dimX, dimY;

    //Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    //Polar coordinates of the touch point from joystick center
    private double radial;
    private double angle;

    //User coordinates of last touch point
    private int userX, userY;

    //Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private int offsetX;
    private int offsetY;

    public enum SliderOrientation {Vertical, Horizontal}

    ;
    private SliderOrientation sliderOrientation = SliderOrientation.Vertical;

    private float initialXPosAdj = 0;
    private float initialYPosAdj = 0;

    // =========================================
    // Constructors
    // =========================================

    public CrossSlider(Context context) {
        super(context);
        initJoystickView();
    }

    public CrossSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();
    }

    public CrossSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initJoystickView();
    }

    // =========================================
    // Initialization
    // =========================================

    private void initJoystickView() {
        setFocusable(true);

        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setColor(Color.GRAY);
        paintBackground.setStrokeWidth(1);
        paintBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        paintThumbstick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintThumbstick.setColor(Color.DKGRAY);
        paintThumbstick.setStrokeWidth(1);
        paintThumbstick.setStyle(Paint.Style.FILL_AND_STROKE);

        paintOuterCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOuterCircle.setColor(Color.LTGRAY);
        paintOuterCircle.setStrokeWidth(25);
        paintOuterCircle.setStyle(Paint.Style.STROKE);

        paintBackgroundCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackgroundCircle.setColor(Color.LTGRAY);
        paintBackgroundCircle.setStrokeWidth(1);
        paintBackgroundCircle.setStyle(Paint.Style.FILL);

        setMovementRange(10);
        setMoveResolution(1.0f);
        setClickThreshold(0.4f);
        setYAxisInverted(true);
        setUserCoordinateSystem(COORDINATE_CARTESIAN);
        setAutoReturnToCenter(true);
        setVisibility(View.VISIBLE);
    }

    public void setThumbstickColor(int color) {
        paintThumbstick.setColor(color);
        invalidate();
    }

    public void setOuterCircleColor(int color) {
        paintOuterCircle.setColor(color);
        invalidate();
    }

    public void setBackgroundCircleColor(int color) {
        paintBackgroundCircle.setColor(color);
        invalidate();
    }

    public void setLineColor(int color) {
        paintBackground.setColor(color);
        invalidate();
    }

    public void setAutoReturnToCenter(boolean xAxisAutoReturnToCenter) {
        this.xAxisAutoReturnToCenter = xAxisAutoReturnToCenter;
        this.yAxisAutoReturnToCenter = xAxisAutoReturnToCenter;
    }

    public boolean isXAxisAutoReturnToCenter() {
        return xAxisAutoReturnToCenter;
    }

    public boolean isYAxisAutoReturnToCenter() {
        return yAxisAutoReturnToCenter;
    }

    public void setUserCoordinateSystem(int userCoordinateSystem) {
        if (userCoordinateSystem < COORDINATE_CARTESIAN || movementConstraint > COORDINATE_DIFFERENTIAL)
            Log.e(TAG, "invalid value for userCoordinateSystem");
        else
            this.userCoordinateSystem = userCoordinateSystem;
    }

    public int getUserCoordinateSystem() {
        return userCoordinateSystem;
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
        return xAxisInverted;
    }

    public boolean isYAxisInverted() {
        return yAxisInverted;
    }

    public void setXAxisInverted(boolean xAxisInverted) {
        this.xAxisInverted = xAxisInverted;
    }

    public void setYAxisInverted(boolean yAxisInverted) {
        this.yAxisInverted = yAxisInverted;
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

    public void setSliderOrientation(SliderOrientation orientationUse) {
        sliderOrientation = orientationUse;
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

    public void setPosition(float initXPosAdj) {
        initialXPosAdj = initXPosAdj;
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        dimX = getMeasuredWidth();
        dimY = getMeasuredHeight();

        cX = dimX / 2;
        cY = dimY / 2;

        Log.d(TAG, "onWindowFocusChanged cX: " + cX + " cY: " + cY + " dimX: " + dimX + " dimY: " + dimY);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        dimX = getMeasuredWidth();
        dimY = getMeasuredHeight();

        cX = dimX / 2;
        cY = dimY / 2;

        Log.d(TAG, "onLayout cX: " + cX + " cY: " + cY + " dimX: " + dimX + " dimY: " + dimY);


        radiusThumbstick = (int) (40);
        radiusBackground = radiusThumbstick * 7.3f;
        radiusOutline = radiusThumbstick * 7.5f;
        int handleInnerBoundaries = radiusThumbstick;
        movementRadius = Math.max(cX, cY) - handleInnerBoundaries;
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
        canvas.drawCircle(cX, cY, radiusBackground, paintBackgroundCircle);

        // Draw the outside of the background circle
        canvas.drawCircle(cX, cY, radiusOutline, paintOuterCircle);

        // Draw horizontal axis
        canvas.drawRect(cX - 285, cY - 15, cX + 285, cY + 15, paintBackground);

        // Draw vertical axis
        canvas.drawRect(cX - 15, cY - 285, cX + 15, cY + 285, paintBackground);

        Log.v(TAG, "cX: " + cX + " cY: " + cY + " dimX: " + dimX + " dimY: " + dimY);

        if (initialXPosAdj != 0 || initialYPosAdj != 0) {
            if (!xAxisAutoReturnToCenter) {
                touchX = movementRadius * initialXPosAdj;
            }

            if (!yAxisAutoReturnToCenter) {
                touchY = movementRadius * -initialYPosAdj;
            }
            initialXPosAdj = 0;
            initialYPosAdj = 0;

            reportOnMoved();
        }

        handleX = touchX + cX;
        handleY = touchY + cY;

        Log.v(TAG, "touchX: " + touchX + " touchY: " + touchY);


        // Draw the handle
        canvas.drawCircle(handleX, handleY, radiusThumbstick, paintThumbstick);


//              Log.d(TAG, String.format("touch(%f,%f)", touchX, touchY));
//              Log.d(TAG, String.format("onDraw(%.1f,%.1f)\n\n", handleX, handleY));
        canvas.restore();
    }

    // Constrain touch within a box
    private void constrainBox() {
        touchX = Math.max(Math.min(touchX, movementRadius), -movementRadius);
        touchY = Math.max(Math.min(touchY, movementRadius), -movementRadius);
    }

    // Constrain touch within a circle
    private void constrainCircle() {
        float diffX = touchX;
        float diffY = touchY;
        double radial = Math.sqrt((diffX * diffX) + (diffY * diffY));
        if (radial > movementRadius) {
            touchX = (int) ((diffX / radial) * movementRadius);
            touchY = (int) ((diffY / radial) * movementRadius);
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

        // Detect if user is trying to move horizontally or vertically
        if (Math.abs(touchX) > Math.abs(touchY)) {
            touchY = 0;
        } else {
            touchX = 0;
        }

        calcUserCoordinates();

        Log.v(TAG, "touchX: " + touchX + " touchY: " + touchY);

        if (moveListener != null) {
            boolean rx = Math.abs(touchX - reportX) >= moveResolution;
            boolean ry = Math.abs(touchY - reportY) >= moveResolution;
            if (rx || ry) {

                this.reportX = touchX;
                this.reportY = touchY;

//                              Log.d(TAG, String.format("moveListener.OnMoved(%d,%d)", (int)userX, (int)userY));
                moveListener.onMoved(userX, userY);
            }
        }
    }

    private void calcUserCoordinates() {
        //First convert to cartesian coordinates
        cartX = (int) (touchX / movementRadius * movementRange);
        cartY = (int) (touchY / movementRadius * movementRange);

        radial = Math.sqrt((cartX * cartX) + (cartY * cartY));
        angle = Math.atan2(cartY, cartX);

        //Invert X axis if requested
        if (!xAxisInverted)
            cartX *= -1;

        //Invert Y axis if requested
        if (!yAxisInverted)
            cartY *= -1;

        if (userCoordinateSystem == COORDINATE_CARTESIAN) {
            userX = cartX;
            userY = cartY;
        } else if (userCoordinateSystem == COORDINATE_DIFFERENTIAL) {
            userX = cartY + cartX / 4;
            userY = cartY - cartX / 4;

            if (userX < -movementRange)
                userX = (int) -movementRange;
            if (userX > movementRange)
                userX = (int) movementRange;

            if (userY < -movementRange)
                userY = (int) -movementRange;
            if (userY > movementRange)
                userY = (int) movementRange;
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
        if (xAxisAutoReturnToCenter || yAxisAutoReturnToCenter) {
            final int numFrames = 5;
            final double intervalX = (xAxisAutoReturnToCenter ? (0 - touchX) / numFrames : 0);
            final double intervalY = (yAxisAutoReturnToCenter ? (0 - touchY) / numFrames : 0);

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
