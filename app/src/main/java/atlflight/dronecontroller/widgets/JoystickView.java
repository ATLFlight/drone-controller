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

import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class JoystickView extends View {

  private class LimitChannel {
    public LimitChannel(float x11, float y11, float x12, float y12,
                        float x21, float y21, float x22, float y22) {
      mP11 = new Point((int)x11, (int)y11);
      mP12 = new Point((int)x12, (int)y12);
      mP21 = new Point((int)x21, (int)y21);
      mP22 = new Point((int)x22, (int)y22);
    }

    public void draw(Canvas canvas, Paint paint) {
      canvas.drawLine(mP11.x, mP11.y, mP12.x, mP12.y, paint);
      canvas.drawLine(mP21.x, mP21.y, mP22.x, mP22.y, paint);
    }

    public Point mP11;
    public Point mP12;
    public Point mP21;
    public Point mP22;
  }

  private final int MIN_VIEW_SQUARE_SIZE = 300;
  private final int MAX_VIEW_SQUARE_SIZE = 700;

  private int size = MAX_VIEW_SQUARE_SIZE;

  private Point origin;

  private int startXPos;
  private int startYPos;

  private Paint paintThumbstick;
  private Paint paintRectangle;
  private Paint paintLine;
  private Paint paintOuterCircle;
  private Paint paintBackgroundCircle;

  private boolean xReturnToCenter = true;
  private boolean yReturnToCenter = true;

  private boolean isXInverted = false;
  private boolean isYInverted = false;

  private int knobRadius;
  private int moveRadius;

  private final int   strokeWidth = 1;
  private final int strokeWidthRectangle = 25;
  private final float knobDiameterPercentage = 0.10f;

  private float absX;
  private float absY;

  private float initialX = 0;
  private float initialY = 0;

  private int winBottom;

  private int mTopMargin;

  private boolean mActive = true;

  private JoystickMovedListener onMovedListener = null;

  private Context context;

  private LimitChannel mXAxisLimitChannel;
  private LimitChannel mYAxisLimitChannel;

  private boolean mXAxisLimited = false;
  private boolean mYAxisLimited = false;

  public JoystickView(Context context) {
    super(context);
    initView();
  }

  public JoystickView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    initView();
  }

  public JoystickView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.context = context;
    initView();
  }

  public void activate(boolean en) {
    mActive = en;
    setAlpha( en ? 1.0f : 0.65f );
    invalidate();
  }

  public void limitAxis(boolean limitX, boolean limitY) {
    mXAxisLimited = limitX;
    mYAxisLimited = limitY;
    invalidate();
  }

  private void initView() {

    paintRectangle = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintRectangle.setStrokeWidth(strokeWidthRectangle);
    paintRectangle.setStyle(Paint.Style.STROKE);
    paintRectangle.setColor(Color.TRANSPARENT);

    paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintLine.setStrokeWidth(strokeWidth);
    paintLine.setStyle(Paint.Style.STROKE);
    paintLine.setColor(Color.GREEN);


    paintThumbstick = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintThumbstick.setStrokeWidth(strokeWidth);
    paintThumbstick.setStyle(Paint.Style.FILL_AND_STROKE);
    paintThumbstick.setColor(Color.DKGRAY);

    paintOuterCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintOuterCircle.setStrokeWidth(strokeWidthRectangle);
    paintOuterCircle.setStyle(Paint.Style.STROKE);
    paintOuterCircle.setColor(Color.LTGRAY);

    paintBackgroundCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBackgroundCircle.setStrokeWidth(strokeWidthRectangle);
    paintBackgroundCircle.setStyle(Paint.Style.FILL);
    paintBackgroundCircle.setColor(Color.LTGRAY);

    origin = new Point(size/2, size/2);

    WindowManager wm = (WindowManager)context.getSystemService(Service.WINDOW_SERVICE);
    Point sz = new Point();
    wm.getDefaultDisplay().getSize(sz);

    mTopMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        10,
        getResources().getDisplayMetrics());

    winBottom = sz.y - mTopMargin;

    setDimension(MAX_VIEW_SQUARE_SIZE);
  }

  public void setRectangleColor(int color) {
    paintRectangle.setColor(color);
    invalidate();
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

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(size, size);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    canvas.drawCircle(size/2, size/2, knobRadius*7.5f, paintOuterCircle);
    canvas.drawCircle(size/2, size/2, knobRadius*7.3f, paintBackgroundCircle);
    canvas.drawCircle(absX, absY, knobRadius, paintThumbstick);
    canvas.drawLine(origin.x, origin.y, absX, absY, paintLine);

    if (mXAxisLimited) {
      mXAxisLimitChannel.draw(canvas, paintOuterCircle);
    }

    if (mYAxisLimited) {
      mYAxisLimitChannel.draw(canvas, paintOuterCircle);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    reset();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

    if (event == null) return false;

    if (!mActive) {
      return true;
    }

    int maskedAction = event.getAction();
    boolean rptMovement = false;

    switch (maskedAction) {
      //returns joystick to center when no touch is detected
      case MotionEvent.ACTION_UP: {
        if (xReturnToCenter) {
          absX = origin.x;
        }

        if(yReturnToCenter) {
          absY = origin.y;
        }
        rptMovement = true;
        break;
      }
      //fires ACTION_MOVE when movement has occurred
      case MotionEvent.ACTION_MOVE: {
        if (!mXAxisLimited) {
          absX = Math.min(Math.max(event.getX(), knobRadius), size - knobRadius);
          rptMovement = true;
        }
        if (!mYAxisLimited) {
          absY = Math.min(Math.max(event.getY(), knobRadius), size - knobRadius);
          rptMovement = true;
        }
        break;
      }
    }
    if (rptMovement) {
      reportMovement();
    }
    return true;
  }

  public void setOnMovedListener(JoystickMovedListener onMovedListener) {
    this.onMovedListener = onMovedListener;
  }

  public void setAutoReturnToCenter(boolean xReturnToCenter, boolean yReturnToCenter ) {
    boolean update = this.xReturnToCenter != xReturnToCenter ||
        this.yReturnToCenter != yReturnToCenter;

    this.xReturnToCenter = xReturnToCenter;
    this.yReturnToCenter = yReturnToCenter;

    if (update) {

      if (this.xReturnToCenter) {
        absX = origin.x;
      }

      if (this.yReturnToCenter) {
        absY = origin.y;
      }

      reportMovement();

      invalidate();
    }
  }

  public void invertAxis(boolean isXInverted, boolean isYInverted) {
    this.isXInverted = isXInverted;
    this.isYInverted = isYInverted;
  }

  public void setInitialPosition(float x, float y) {

    if (x > 1) {
      initialX = 1;
    } else if (x < -1) {
      initialX = -1;
    } else {
      initialX = x;
    }

    if (y > 1) {
      initialY = 1;
    } else if (y < -1) {
      initialY = -1;
    } else {
      initialY = y;
    }
    initialY = -initialY;

    startXPos = (int)(((1+initialX)/2) * (this.size));

    if (startXPos > (this.size - knobRadius - strokeWidthRectangle /2)) {
      startXPos = this.size - knobRadius - strokeWidthRectangle /2;
    } else if (startXPos < (0 + knobRadius + strokeWidthRectangle /2)) {
      startXPos = 0 + knobRadius + strokeWidthRectangle /2;
    }

    startYPos = (int)(((1+initialY)/2) * (this.size));

    if (startYPos > (this.size - knobRadius - strokeWidthRectangle /2)) {
      startYPos = this.size - knobRadius - strokeWidthRectangle /2;
    } else if (startYPos < (0 + knobRadius + strokeWidthRectangle /2)) {
      startYPos = 0 + knobRadius + strokeWidthRectangle /2;
    }

  }

  public void reset() {
    if (xReturnToCenter) {
      absX = origin.x;
    }
    else {
      absX = startXPos;
    }

    if (yReturnToCenter) {
      absY = origin.y;
    } else {
      absY = startYPos;
    }
    reportMovement();
  }

  public void setPosition(float x, float y) {
    absX = (moveRadius * x)  + origin.x;
    absY = (-moveRadius * y) + origin.y;

    invalidate();
  }

  public void setXPosition(float x) {
    absX = (moveRadius * x)  + origin.x;
    invalidate();
  }

  public void setYPosition(float y) {
    absY = (-moveRadius * y) + origin.y;
    invalidate();
  }

  private void reportMovement() {
    invalidate();

    float x = ((absX-origin.x)/moveRadius);
    float y = ((absY-origin.y)/-moveRadius);

    if (onMovedListener != null) {
      if(isXInverted) {
        x = -x;
      }
      if(isYInverted) {
        y = -y;
      }

      onMovedListener.onMoved(x, y);
    }
  }

  public void setLayoutSize(int size) {
    setDimension(size - mTopMargin);
    invalidate();
    requestLayout();
  }

  private void setDimension(int size) {
    if (size < MIN_VIEW_SQUARE_SIZE) {
      this.size = MIN_VIEW_SQUARE_SIZE;
    } else if (size > MAX_VIEW_SQUARE_SIZE) {
      this.size = MAX_VIEW_SQUARE_SIZE;
    } else {
      this.size = size;
    }

    int winWidth = getWinSize().x;

    if (this.size > winWidth / 3) {
      this.size = winWidth / 3;
    }

    origin.x = this.size/2;
    origin.y = this.size/2;

    knobRadius = 40;
    moveRadius = Math.min(this.size, this.size)/2 - knobRadius;

    float offset = knobRadius + paintRectangle.getStrokeWidth();

    mXAxisLimitChannel =
        new LimitChannel(origin.x - offset, 0, origin.x - offset, size,
            origin.x + offset, 0, origin.y + offset, size);

    mYAxisLimitChannel =
        new LimitChannel(0, origin.y - offset, size, origin.y - offset,
            0, origin.y + offset, size, origin.y + offset);

    startXPos = (int)(((1+initialX)/2) * (this.size));
    startYPos = (int)(((1+initialY)/2) * (this.size));

    if (startXPos < knobRadius) {
      startXPos = knobRadius;
    } else if (startXPos > this.size - knobRadius) {
      startXPos = this.size - knobRadius;
    }

    if (startYPos < knobRadius) {
      startYPos = knobRadius;
    } else if (startYPos > this.size - knobRadius) {
      startYPos = this.size - knobRadius;
    }

    setMeasuredDimension(this.size, this.size);
  }

  public void getWinTopAndBottom(int[] topAndBot) {
    int[] locInWin = new int[ 2 ];
    getLocationInWindow(locInWin);
    topAndBot[0] = locInWin[1];
    topAndBot[1] = getWinSize().y - mTopMargin;
  }

  private Point getWinSize() {
    Point size = new Point();
    WindowManager wm = (WindowManager)context.getSystemService(Service.WINDOW_SERVICE);
    wm.getDefaultDisplay().getSize(size);
    return size;
  }
}

