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

package atlflight.dronecontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import atlflight.MavlinkWrapper;
import atlflight.dronecontroller.widgets.CrossSlider;
import atlflight.dronecontroller.widgets.GeneralSlider;
import atlflight.dronecontroller.widgets.JoystickMovedListener;
import atlflight.dronecontroller.widgets.JoystickView;
import atlflight.dronectrlapi.DroneCtrlApi;
import atlflight.connectionlistener.ConnectionListener;

import java.net.InetSocketAddress;

public class MainActivity extends Activity implements DroneCtrlApi.BatteryStatusListener, SurfaceHolder.Callback {

  private static final String TAG = "DroneController";
  private static final int RESULT_SETTINGS = 1;

  private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
  private StreamHandler mStreamHandler;
  static final private int BUFFER_SIZE = 460800;

  @Override
  public void notify(final DroneCtrlApi.BatteryStatus batteryStatus) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          float value = batteryStatus.mVoltages[0];
          if (value <= 690) {
            battery_icon.setImageResource(R.drawable.icon_battery0);
          }
          else if (value > 690 && value <= 710) {
            battery_icon.setImageResource(R.drawable.icon_battery50);
          } else {
            battery_icon.setImageResource(R.drawable.icon_battery100);
          }
        }
      });
  }

  private enum JoystickType {
    JOYSTICK_TYPE_FULL,
    JOYSTICK_TYPE_SLIDER,
    JOYSTICK_TYPE_CROSS
  }

  /// Type of joystick being used
  private JoystickType joystickType = JoystickType.JOYSTICK_TYPE_FULL;

  public enum FlightController {
    SNAV,
    PX4
  }

  String INTERFACE_TYPE_UDP;
  String INTERFACE_TYPE_BT;

  private FlightController flightController = FlightController.PX4;

  private enum BackgroundType {
    BACKGROUND_TYPE_PLAIN,
    BACKGROUND_TYPE_FPV_STREAM
  }

  /// Background of main activity
  private BackgroundType backgroundType = BackgroundType.BACKGROUND_TYPE_FPV_STREAM;

  private Context context;

  FlightModeMapping mFlightModeMapping = new PX4FlightModeMapping();

  private float STARTING_THRUST = 0.5f;

  // This flag is used to start RTSP client on devices other than SAMSUNG.
  // For SAMSUNG devices, the native RTSP Client is good and does not need the streamhandler JNI
  // wrapper.
  private boolean SAMSUNG = false;

  InetSocketAddress mDroneAddr = new InetSocketAddress( 0 );

  private TextView       mSSIDOrBdAddrValue;

  private float desRoll;
  private float desPitch;
  private float desYaw;
  private float desThrust = STARTING_THRUST;
  private float rollPitchGain = 0.45f;
  private float thrustGain = 1.0f;
  private float yawGain = 0.25f;

  private LinearLayout  mRPYTLineLayout;
  private TextView      mRPYTString;
  private LinearLayout  mFlightModeLineLayout;
  private TextView      mFlightModeString;
  private ImageView battery_icon;

  DroneCtrlApi.Setpoint mSetpoint = new DroneCtrlApi.Setpoint();
  DroneCtrlApi.CommandLong mCommandLong = new DroneCtrlApi.CommandLong();

  private DroneCtrlApi  mDroneCtrlApi;
  private int           mTxIntervalMs;

  private boolean       mQuitting    = false;
  private boolean       mQuitEnabled = true;
  private boolean       landingNow = false;

  private final int     mVirtualJoystickSteps = 128;

  private boolean       slidersAvailable = true;

  private String             mInterfaceType;
  private ConnectionListener mConnectionListener;
  private String             mBTDeviceBdAddr;


    @Override
  protected void onCreate( Bundle savedInstanceState )
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_swap);

    Log.e(TAG, "onCreate");

    Log.v(TAG, "device: " + Build.MANUFACTURER + ", " + Build.MODEL);
    if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
      SAMSUNG = true;
    }

    context = this;

    INTERFACE_TYPE_UDP = getString(R.string.comm_interface_type_udp);
    INTERFACE_TYPE_BT  = getString(R.string.comm_interface_type_bluetooth);
    mInterfaceType     = getString(R.string.comm_interface_type_udp);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(false);
    builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

      }
    });
    Spanned message = Html.fromHtml("Warning: flying a multi-rotor aircraft can be dangerous. Fly only aircraft " +
    "that are in good operating condition and in open areas free of visual obstructions and " +
            "hazards. <br/> <br/> <b> Wear eye protection at all times.</b>" +
            " <br/> <br/> The user of this software accepts the risks inherent with operating a " +
            "multi-rotor aircraft.");
    builder.setMessage(message).create().show();

    if (!SAMSUNG) {
      SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
      SurfaceHolder sh = sv.getHolder();
      sh.addCallback(this);

      mStreamHandler = new StreamHandler(this, this);
    }

    mConnectionListener = new ConnectionListener() {
      @Override
      public void onStateChange(final String deviceAddr, final ConnectionState newState) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            switch (newState) {
              case CONNECTED:
                mSSIDOrBdAddrValue.setText("BT device: " + deviceAddr + " connected");
                break;
              case CONNECTING:
                mSSIDOrBdAddrValue.setText("Connecting to BT device: " + deviceAddr + "...");
                break;
              case DISCONNECTED:
                mSSIDOrBdAddrValue.setText("");
                break;
            }
          }
        });
      }
    };

    mSSIDOrBdAddrValue = ( TextView ) findViewById( R.id.ssid_or_bt_info);
    mRPYTLineLayout = ( LinearLayout ) findViewById( R.id.rpyt_line );
    mRPYTString = ( TextView ) findViewById( R.id.rpyt_string );
    mFlightModeLineLayout = (LinearLayout) findViewById(R.id.rpyt_line);
    mFlightModeString = (TextView) findViewById(R.id.flight_mode);

    battery_icon = (ImageView) findViewById(R.id.battery_icon);

    mDroneCtrlApi = new DroneCtrlApi()
    {

      @Override
      public CommandLong getCommandLong() {
        mCommandLong.command = MavlinkWrapper.MAV_CMD.DO_SET_MODE.mode;
        mCommandLong.confirmation = 1;
        return mCommandLong;
      }

      @Override
      public Setpoint getSetpoint()
      {
        if (mSetpoint.mPitch>1.0)  mSetpoint.mPitch = (float)1.0;
        if (mSetpoint.mPitch<-1.0) mSetpoint.mPitch = (float)-1.0;
        if (mSetpoint.mRoll>1.0)   mSetpoint.mRoll = (float)1.0;
        if (mSetpoint.mRoll<-1.0)  mSetpoint.mRoll = (float)-1.0;

        //mSetpoint.mYaw              = 0;
        //mSetpoint.mThrust           = 0;
        //mSetpoint.mFlightModeSwitch = 0;
        printRPYT( mSetpoint.mRoll, mSetpoint.mPitch, mSetpoint.mYaw, mSetpoint.mThrust, mSetpoint.mFlightModeSwitch );

        final float thresh = .2f;

        if(slidersAvailable) {
          if(Math.abs(desRoll)<thresh && Math.abs(desPitch)<thresh)
          {
            mSetpoint.mRoll = 0;
            mSetpoint.mPitch = 0;
          }
          else
          {
            mSetpoint.mRoll = rollPitchGain*(Math.max(Math.abs(desRoll)-thresh,0f))*Math.signum(desRoll);
            mSetpoint.mPitch = rollPitchGain*(Math.max(Math.abs(desPitch)-thresh,0f))*Math.signum(desPitch);
          }

          mSetpoint.mYaw = yawGain * desYaw;
          mSetpoint.mThrust = (thrustGain) * (desThrust - .5f) + .5f;
        }

        if (mSetpoint.mPitch>1.0) mSetpoint.mPitch = (float)1.0;
        if (mSetpoint.mPitch<-1.0) mSetpoint.mPitch = (float)-1.0;
        if (mSetpoint.mRoll>1.0) mSetpoint.mRoll = (float)1.0;
        if (mSetpoint.mRoll<-1.0) mSetpoint.mRoll = (float)-1.0;

        if (mSetpoint.mThrust<0.0) mSetpoint.mThrust = (float)0.0;
        if (mSetpoint.mThrust>1.0) mSetpoint.mThrust = (float)1.0;

        printRPYT(mSetpoint.mRoll, mSetpoint.mPitch, mSetpoint.mYaw, mSetpoint.mThrust, mSetpoint.mFlightModeSwitch);


        Setpoint finalSetpoint = new Setpoint();
        finalSetpoint.mRoll = mSetpoint.mRoll;
        finalSetpoint.mPitch = mSetpoint.mPitch;
        finalSetpoint.mYaw = mSetpoint.mYaw;
        finalSetpoint.mThrust = mSetpoint.mThrust;
        finalSetpoint.mFlightModeSwitch = mSetpoint.mFlightModeSwitch;

        if (flightController == FlightController.SNAV) {
          // SNAV needs these bit fields zeroed out after they're sent, so you can hit the "takeoff" button twice,
          // and the "takeoff" bit will be sent twice.
          mSetpoint.mFlightModeSwitch = 0;
        }

        return finalSetpoint;
      }
    };

    mDroneCtrlApi.setTxInterval(mTxIntervalMs);

    // Pull settings from shared prefs
    updateSettings();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    // When the window loses focus (e.g., the action overflow is shown),
    // cancel any pending hide action. When the window gains focus,
    // hide the system UI.
    if (hasFocus) {
      int currentApiVersion = Build.VERSION.SDK_INT;

      if (currentApiVersion >= Build.VERSION_CODES.KITKAT){
        // Immersive flag only works on API 19 and above.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
      }
    }

  }

  @Override
  public void onPause()
  {
    super.onPause();
    quit();
  }

  private void handlePX4ButtonSetup() {
    Button buttonArm = (Button) findViewById(R.id.button_px4_arm);
    Button buttonDisarm = (Button) findViewById(R.id.button_px4_disarm);
    Button buttonAltCtl = (Button) findViewById(R.id.button_px4_altctl);
    Button buttonManualCtl = (Button) findViewById(R.id.button_px4_manual);

    buttonArm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandLong.param1 = mCommandLong.param1 | MavlinkWrapper.MAV_MODE_FLAG.SAFETY_ARMED.flag;
        Log.e(TAG, "arm control");
        mDroneCtrlApi.startCommandLongTxThread();
      }
    });

    buttonDisarm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandLong.param1 = mCommandLong.param1 & ~MavlinkWrapper.MAV_MODE_FLAG.SAFETY_ARMED.flag; // arm
        Log.e(TAG, "disarm control");
        mDroneCtrlApi.startCommandLongTxThread();
      }
    });

    buttonAltCtl.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandLong.param1 = mCommandLong.param1 | MavlinkWrapper.MAV_MODE_FLAG.CUSTOM_MODE_ENABLED.flag;
        mCommandLong.param2 = MavlinkWrapper.PX4_CUSTOM_MAIN_MODE.ALTCTL.mode;
        Log.e(TAG, "altitude control");
        mDroneCtrlApi.startCommandLongTxThread();
      }
    });

    buttonManualCtl.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mCommandLong.param1 = mCommandLong.param1 | MavlinkWrapper.MAV_MODE_FLAG.CUSTOM_MODE_ENABLED.flag;
        mCommandLong.param2 = MavlinkWrapper.PX4_CUSTOM_MAIN_MODE.MANUAL.mode;
        Log.e(TAG, "manual");
        mDroneCtrlApi.startCommandLongTxThread();
      }
    });
  }

  private void handleSnavButtonSetup() {
    Button buttonSnavTakeoff = ( Button ) findViewById( R.id.button_snav_takeoff);
    Button buttonSnavLand = (Button)findViewById(R.id.button_snav_land);

    buttonSnavTakeoff.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mSetpoint.mFlightModeSwitch = (short) (mSetpoint.mFlightModeSwitch | SnavFlightModeMapping.SNAV_BUTTON_MAP_5_TAKEOFF_SWITCH);
        mSetpoint.mFlightModeSwitch = (short) (mSetpoint.mFlightModeSwitch & ~SnavFlightModeMapping.SNAV_BUTTON_MAP_4_LAND_SWITCH);
        Log.e(TAG, "takeoff");
      }
    });

    buttonSnavLand.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mSetpoint.mFlightModeSwitch = (short) (mSetpoint.mFlightModeSwitch | SnavFlightModeMapping.SNAV_BUTTON_MAP_4_LAND_SWITCH);
        mSetpoint.mFlightModeSwitch = (short) (mSetpoint.mFlightModeSwitch & ~SnavFlightModeMapping.SNAV_BUTTON_MAP_5_TAKEOFF_SWITCH);
        Log.e(TAG, "land");
      }
    });
  }

  @Override
  public void onResume()
  {
    super.onResume();
    Log.e(TAG, "onResume");

    if (!mDroneCtrlApi.isConnected()) {
      if (mInterfaceType.equals(INTERFACE_TYPE_UDP)) {
        mDroneCtrlApi.connect(0, mDroneAddr);
      } else if (mInterfaceType.equals(INTERFACE_TYPE_BT)) {
        mDroneCtrlApi.connect(getApplicationContext(), mBTDeviceBdAddr, mConnectionListener);
      }
    }

    TextView flightControllerInfo = (TextView) findViewById(R.id.flight_controller_info);
    flightControllerInfo.setText(flightController.toString());

    final ViewGroup commandButtons = (ViewGroup) findViewById(R.id.layout_command_buttons);
    commandButtons.removeAllViews();
    if (flightController == FlightController.SNAV) {
      commandButtons.addView(View.inflate(this, R.layout.command_buttons_default, null));
      handleSnavButtonSetup();
      mFlightModeMapping = new SnavFlightModeMapping();
    } else if (flightController == FlightController.PX4) {
      commandButtons.addView(View.inflate(this, R.layout.command_buttons_px4, null));
      handlePX4ButtonSetup();
      mFlightModeMapping = new PX4FlightModeMapping();
    }

    if (backgroundType == BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {

      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
      String ipAddr = sp.getString(getString(R.string.pref_key_drone_ip_addr),
              getString(R.string.pref_default_drone_ip_addr));

      LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);
      mainLayout.setBackgroundColor(Color.BLACK);

      LinearLayout joystickHolder = (LinearLayout) findViewById(R.id.layout_joystick_holder);
      joystickHolder.setBackgroundColor(Color.TRANSPARENT);

      if (!SAMSUNG) {
        boolean retVal = mStreamHandler.init(this, "rtsp://"+ipAddr+"/fpvview", BUFFER_SIZE, null);
        mStreamHandler.play();
      } else {
        VideoView vidView = (VideoView) findViewById(R.id.myVideo2);
        String vidAddress = "rtsp://" + ipAddr + "/fpvview";
        Uri vidUri = Uri.parse(vidAddress);
        vidView.setVideoURI(vidUri);
        vidView.start();
      }

    } else {
      if (!SAMSUNG) {
        mStreamHandler.finalizeSH();
      } else {
        View vidView = findViewById(R.id.myVideo2);
        vidView.setVisibility(View.INVISIBLE);
      }

      LinearLayout joystickHolder = (LinearLayout) findViewById(R.id.layout_joystick_holder);
      joystickHolder.setBackgroundColor(getResources().getColor(R.color.controller_background_red));

      LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);
      mainLayout.setBackgroundColor(Color.WHITE);
    }

    // Since left side joystick can change based on setting, update layout here dynamically
    if(joystickType == JoystickType.JOYSTICK_TYPE_FULL) {
      final ViewGroup vg = (ViewGroup) findViewById(R.id.layout_joystick_holder);
      vg.removeAllViews();
      vg.addView(View.inflate(this, R.layout.joystick_layout_full, null));

      JoystickView leftJoystick = (JoystickView)findViewById(R.id.joystickLeftView);
      leftJoystick.setVisibility(View.VISIBLE);
      //                    (invertX , invertY)
      leftJoystick.invertAxis(true, true);
      //                                 (  x  ,   y )
      leftJoystick.setAutoReturnToCenter(true, false);
      leftJoystick.setOnMovedListener(ytJoystickListener);
      leftJoystick.setPosition(0, 0);

      if (backgroundType==BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {
        leftJoystick.setBackgroundCircleColor(Color.parseColor(getString(R.string.transparent_ltgray)));
        leftJoystick.setOuterCircleColor(Color.TRANSPARENT);
        leftJoystick.setThumbstickColor(Color.parseColor(getString(R.string.transparent_green)));
        leftJoystick.setRectangleColor(Color.parseColor(getString(R.string.transparent_dkgray)));
      } else if (backgroundType==BackgroundType.BACKGROUND_TYPE_PLAIN) {
        leftJoystick.setThumbstickColor(getResources().getColor(R.color.controller_thumbstick));
        leftJoystick.setOuterCircleColor(getResources().getColor(R.color.controller_outer_circle));
        leftJoystick.setBackgroundCircleColor(getResources().getColor(R.color.controller_circle_background));
      }
    }
    else if( joystickType == JoystickType.JOYSTICK_TYPE_SLIDER ) {
      final ViewGroup vg = (ViewGroup) findViewById(R.id.layout_joystick_holder);
      vg.removeAllViews();
      vg.addView(View.inflate(this, R.layout.joystick_layout_slider, null));

      GeneralSlider thrustSlider = (GeneralSlider)findViewById(R.id.thrustSlider);
      GeneralSlider yawSlider = (GeneralSlider)findViewById(R.id.yawSlider);

      thrustSlider.setMovementRange(mVirtualJoystickSteps - 1);
      thrustSlider.setAutoReturnToCenter(true);
      thrustSlider.setPosition(0);
      thrustSlider.setOnJoystickMovedListener(thrustSliderListener);
      thrustSlider.setSliderOrientation(GeneralSlider.SliderOrientation.Vertical);

      yawSlider.setMovementRange(mVirtualJoystickSteps - 1);
      yawSlider.setAutoReturnToCenter(true);
      yawSlider.setPosition(0);
      yawSlider.setOnJoystickMovedListener(yawSliderListener);
      yawSlider.setSliderOrientation(GeneralSlider.SliderOrientation.Horizontal);

      if (backgroundType==BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {
        thrustSlider.setThumbstickColor(Color.parseColor(getString(R.string.transparent_green)));
        thrustSlider.setLineColor(Color.parseColor(getString(R.string.transparent_dkgray)));
        yawSlider.setThumbstickColor(Color.parseColor(getString(R.string.transparent_green)));
        yawSlider.setLineColor(Color.parseColor(getString(R.string.transparent_dkgray)));
      } else if (backgroundType==BackgroundType.BACKGROUND_TYPE_PLAIN) {
        thrustSlider.setThumbstickColor(getResources().getColor(R.color.controller_thumbstick));
        thrustSlider.setLineColor(getResources().getColor(R.color.controller_outer_circle));
        yawSlider.setThumbstickColor(getResources().getColor(R.color.controller_thumbstick));
        yawSlider.setLineColor(getResources().getColor(R.color.controller_outer_circle));
      }
    }
    else if( joystickType == JoystickType.JOYSTICK_TYPE_CROSS ) {
      final ViewGroup vg = (ViewGroup) findViewById(R.id.layout_joystick_holder);
      vg.removeAllViews();
      vg.addView(View.inflate(this, R.layout.joystick_layout_cross, null));

      CrossSlider ytSlider = (CrossSlider)findViewById(R.id.crossYTSlider);

      ytSlider.setMovementRange(mVirtualJoystickSteps - 1);
      ytSlider.setAutoReturnToCenter(true);
      ytSlider.setPosition(0);
      ytSlider.setOnJoystickMovedListener(ytSliderListener);
      ytSlider.setSliderOrientation(CrossSlider.SliderOrientation.Vertical);

      if (backgroundType==BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {
        ytSlider.setBackgroundCircleColor(Color.TRANSPARENT);
        ytSlider.setThumbstickColor(Color.parseColor(getString(R.string.transparent_green)));
        ytSlider.setLineColor(Color.parseColor(getString(R.string.transparent_dkgray)));
        ytSlider.setOuterCircleColor(Color.TRANSPARENT);
      } else if (backgroundType==BackgroundType.BACKGROUND_TYPE_PLAIN) {
        ytSlider.setLineColor(getResources().getColor(R.color.crosshairs));
        ytSlider.setThumbstickColor(getResources().getColor(R.color.controller_thumbstick));
        ytSlider.setOuterCircleColor(getResources().getColor(R.color.controller_outer_circle));
        ytSlider.setBackgroundCircleColor(getResources().getColor(R.color.controller_circle_background));
      }
    }

    /// Right joystick is always visible
    JoystickView rightJoystick = (JoystickView)findViewById(R.id.joystickRightView);
    rightJoystick.setVisibility(View.VISIBLE);

    if (flightController == FlightController.PX4) {
      //                      (invertX , invertY)
      rightJoystick.invertAxis(false, false);
    } else {
      //                      (invertX , invertY)
      rightJoystick.invertAxis(false, true);
    }
    //                                  (  x  ,   y )
    rightJoystick.setAutoReturnToCenter(true, true);
    rightJoystick.setOnMovedListener(_listenerRight);

    rightJoystick.setPosition(0, 0);
    changeVisibility(rightJoystick, View.VISIBLE);
    if (backgroundType==BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {
      rightJoystick.setBackgroundCircleColor(Color.parseColor(getString(R.string.transparent_ltgray)));
      rightJoystick.setOuterCircleColor(Color.TRANSPARENT);
      rightJoystick.setThumbstickColor(Color.parseColor(getString(R.string.transparent_green)));
      rightJoystick.setRectangleColor(Color.parseColor(getString(R.string.transparent_dkgray)));
    } else if (backgroundType==BackgroundType.BACKGROUND_TYPE_PLAIN) {
      rightJoystick.setThumbstickColor(getResources().getColor(R.color.controller_thumbstick));
      rightJoystick.setOuterCircleColor(getResources().getColor(R.color.controller_outer_circle));
      rightJoystick.setBackgroundCircleColor(getResources().getColor(R.color.controller_circle_background));
    }

    if (backgroundType == BackgroundType.BACKGROUND_TYPE_FPV_STREAM) {
      mRPYTString.setTextColor(Color.WHITE);
      mRPYTString.setBackgroundColor(Color.parseColor(getString(R.string.transparent_dkgray)));
      mFlightModeString.setTextColor(Color.WHITE);
      mFlightModeString.setBackgroundColor(Color.parseColor(getString(R.string.transparent_dkgray)));
      mSSIDOrBdAddrValue.setTextColor(Color.WHITE);
      mSSIDOrBdAddrValue.setBackgroundColor(Color.parseColor(getString(R.string.transparent_dkgray)));
    } else if (backgroundType == BackgroundType.BACKGROUND_TYPE_PLAIN) {
      mRPYTString.setTextColor(Color.WHITE);
      mRPYTString.setBackgroundColor(Color.TRANSPARENT);
      mFlightModeString.setTextColor(Color.WHITE);
      mFlightModeString.setBackgroundColor(Color.TRANSPARENT);
      mSSIDOrBdAddrValue.setTextColor(Color.WHITE);
      mSSIDOrBdAddrValue.setBackgroundColor(Color.TRANSPARENT);
    }

    printRPYTString("Flight API is disabled");
  }

  // Handle settings update/change.
  @Override
  protected void onActivityResult( int requestCode, int resultCode, Intent data )
  {
    super.onActivityResult(requestCode, resultCode, data);

    switch ( requestCode )
    {
    case RESULT_SETTINGS:
      mQuitEnabled = true;
      updateSettings();
      break;

    }
  }

  // Reads the current settings out of the shared preferences and updates the
  // corresponding app variables.
  private void updateSettings()
  {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( this );
    String ipAddr = sp.getString(getString(R.string.pref_key_drone_ip_addr),
                                 getString(R.string.pref_default_drone_ip_addr));
    int port = Integer.valueOf(sp.getString(getString(R.string.pref_key_mavlink_udp_port),
                                            getString(R.string.pref_default_mavlink_udp_port)));

    String interfaceType = sp.getString(getString(R.string.pref_key_comm_interface_type),
                                        getString(R.string.pref_default_comm_interface_type));

    String btDeviceBdAddr = sp.getString(getString(R.string.pref_key_drone_bluetooth_bdaddr),
                                         getString(R.string.pref_default_drone_bluetooth_bdaddr));

    // If there's been a change in interface type, droneIp address,
    // port or Bluetooth device name then we need to update.
    if (!mInterfaceType.equals(interfaceType)        ||
        !mDroneAddr.getHostString().equals( ipAddr ) ||
        mDroneAddr.getPort() != port                 ||
        !btDeviceBdAddr.equals(getString(R.string.pref_default_drone_bluetooth_bdaddr)) &&
            !mBTDeviceBdAddr.equals(btDeviceBdAddr))

      mDroneAddr = new InetSocketAddress( ipAddr, port );
      mBTDeviceBdAddr = btDeviceBdAddr;

      if (mDroneCtrlApi != null) {
        mDroneCtrlApi.disconnect();
      }

      if (interfaceType.equals(INTERFACE_TYPE_UDP)) {
        mWifiStateHandler.postDelayed(checkWifiSSID, 1000);
        mInterfaceType = INTERFACE_TYPE_UDP;
      } else if (interfaceType.equals(INTERFACE_TYPE_BT)) {
        mInterfaceType = INTERFACE_TYPE_BT;
        mWifiStateHandler.removeCallbacks(checkWifiSSID);
      }

      mTxIntervalMs =
          Integer.valueOf( sp.getString( getString(R.string.pref_key_flightctrl_cmd_tx_interval),
                                                   getString(R.string.pref_default_flightctrl_cmd_tx_interval) ) );

      if ( mDroneCtrlApi != null )
      {
        mDroneCtrlApi.setTxInterval( mTxIntervalMs );
      }

      String flightCtrlStr = sp.getString(getString(R.string.pref_key_flight_controller),
                                          getString(R.string.pref_flight_controller_px4));
      if (flightCtrlStr.equalsIgnoreCase(getString(R.string.pref_flight_controller_snav))) {
        flightController = FlightController.SNAV;
      } else if (flightCtrlStr.equalsIgnoreCase(getString(R.string.pref_flight_controller_px4))) {
        flightController = FlightController.PX4;
      }

      String joystickTypeStr = sp.getString(getString(R.string.pref_key_joystick_type),
                                            getString(R.string.pref_default_joystick_type) );
      if (joystickTypeStr.equalsIgnoreCase(getString(R.string.joystick_type_full))) {
        joystickType = JoystickType.JOYSTICK_TYPE_FULL;
      }
      else if (joystickTypeStr.equalsIgnoreCase(getString(R.string.joystick_type_slider))) {
        joystickType = JoystickType.JOYSTICK_TYPE_SLIDER;
      }
      else if (joystickTypeStr.equalsIgnoreCase(getString(R.string.joystick_type_cross))) {
        joystickType = JoystickType.JOYSTICK_TYPE_CROSS;
      }

      String backgroundTypeStr = sp.getString(getString(R.string.pref_key_background),
                                              getString(R.string.pref_background_plain) );
      if (backgroundTypeStr.equalsIgnoreCase(getString(R.string.pref_background_plain))) {
        backgroundType = BackgroundType.BACKGROUND_TYPE_PLAIN;
      } else if (backgroundTypeStr.equalsIgnoreCase(getString(R.string.pref_background_fpv_stream)) ) {
        backgroundType = BackgroundType.BACKGROUND_TYPE_FPV_STREAM;
      }

      yawGain = Float.valueOf(sp.getString(getString(R.string.pref_yaw_gain),
                                           getString(R.string.pref_default_yaw_gain)));

     rollPitchGain = Float.valueOf(sp.getString(getString(R.string.pref_roll_pitch_gain),
            getString(R.string.pref_default_roll_pitch_gain)));
      //printMode( null );
  }

  private void changeVisibility(final View view, final int visibility) {
    if( view != null ) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          view.setVisibility(visibility);
        }
      });
    }
  }

  public void menuButtonClick(View v) {
    //openOptionsMenu();
    final CharSequence menuOptions[] = new CharSequence[] {getString(R.string.menu_option_settings), getString(R.string.menu_option_wifi_scan)};

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Menu");
    builder.setItems(menuOptions, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if (menuOptions[which].equals(getString(R.string.menu_option_settings))) {
          mQuitEnabled = false;

          // When we transition to off the virtual joystick screen
          // set everything to 0.
          mSetpoint.mRoll = 0;
          mSetpoint.mPitch = 0;
          mSetpoint.mYaw = 0;
          mSetpoint.mThrust = 0;

          Intent i = new Intent(context, SettingsActivity.class);
          startActivityForResult(i, RESULT_SETTINGS);
        } else if (menuOptions[which].equals(getString(R.string.menu_option_wifi_scan))) {
          wifiScan();
        }
      }
    });
    builder.show();
  }

  // Stop everything and exit the app.
  private void quit()
  {
    if ( mQuitEnabled && !mQuitting )
    {
      mQuitting = true;
      mDroneCtrlApi.disconnect();
      finish();
    }
  }

  private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

    @Override
    public void onMoved(float x, float y) {
      final float step = 1f/(mVirtualJoystickSteps-1);
      if(slidersAvailable) {
        desPitch = y * step * 150;
        desRoll = x * step * 150;
      }
    }

    @Override
    public void onReleased()
    {
    }

    @Override
    public void onReturnedToCenter()
    {
    }
  };

  private JoystickMovedListener thrustSliderListener = new JoystickMovedListener() {

    @Override
    public void onMoved(float pan, float tilt) {
      final float step = 1f/(mVirtualJoystickSteps-1);
      float temp_thrust = -(tilt*step);
      if (temp_thrust>1.0) temp_thrust= (float)1.0;
      if (temp_thrust<-1.0) temp_thrust= (float)-1.0;

      if(slidersAvailable) {
        desThrust = (temp_thrust + 1) / 2;
      }
    }

    @Override
    public void onReleased()
    {
    }

    @Override
    public void onReturnedToCenter()
    {
    }
  };

  /// Listener for the Yaw and Thrust CrossStick`
  private JoystickMovedListener ytSliderListener = new JoystickMovedListener() {

    @Override
    public void onMoved(float pan, float tilt) {
      final float step = 1f / (mVirtualJoystickSteps - 1);
      float temp_thrust = -(tilt * step);
      if (temp_thrust > 1.0) temp_thrust = (float) 1.0;
      if (temp_thrust < -1.0) temp_thrust = (float) -1.0;

      if (slidersAvailable) {
        desThrust = (temp_thrust + 1) / 2;
      }

      float temp_yaw = -(pan * step);
      if (temp_yaw > 1.0) temp_yaw = (float) 1.0;
      if (temp_yaw < -1.0) temp_yaw = (float) -1.0;

      if (slidersAvailable) {
        desYaw = temp_yaw;
      }

    }

    @Override
    public void onReleased()
    {
    }

    @Override
    public void onReturnedToCenter()
    {
    }
  };

  private JoystickMovedListener ytJoystickListener = new JoystickMovedListener() {

    @Override
    public void onMoved(float pan, float tilt) {
      if (slidersAvailable) {
        desThrust = 0.5f - tilt/2;
        desYaw = -pan;
      }
      Log.v( TAG, "pan: "+pan+ " tilt: " + tilt);
    }

    @Override
    public void onReleased()
    {
    }

    @Override
    public void onReturnedToCenter()
    {
    }
  };

  private JoystickMovedListener yawSliderListener = new JoystickMovedListener() {

    @Override
    public void onMoved(float pan, float tilt) {
      final float step = 1f/(mVirtualJoystickSteps-1);
      float temp_yaw = -(pan*step);
      if (temp_yaw>1.0) temp_yaw= (float)1.0;
      if (temp_yaw<-1.0) temp_yaw= (float)-1.0;

      if(slidersAvailable) {
        desYaw = temp_yaw;
      }
    }

    @Override
    public void onReleased()
    {
    }

    @Override
    public void onReturnedToCenter()
    {
    }
  };

  final Handler mWifiStateHandler = new Handler();
  Runnable checkWifiSSID = new Runnable() {
    @Override
    public void run() {

      /* do what you need to do */
      WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      int Rssi = wifiManager.getConnectionInfo().getRssi();
      WifiInfo wifiInfo = wifiManager.getConnectionInfo();
      SupplicantState ss = wifiInfo.getSupplicantState();

      String wifiState;

      mSSIDOrBdAddrValue.setText("Waiting for SSID...");

      switch( ss )
      {
        case COMPLETED:
          wifiState = "Connected to " + wifiInfo.getSSID()+ String.format("%d",Rssi);
          break;
        case ASSOCIATED:
        case ASSOCIATING:
        case AUTHENTICATING:
        case FOUR_WAY_HANDSHAKE:
        case GROUP_HANDSHAKE:
        case DISCONNECTED:
        case SCANNING:
          wifiState = "WiFi Scanning...";
          break;
        case DORMANT:
          wifiState = "WiFi Dormant";
          break;
        case INACTIVE:
        case INTERFACE_DISABLED:
        case INVALID:
        case UNINITIALIZED:
        default:
          wifiState = "Wifi Disabled";
          break;
      }

      //mSSIDOrBtDevNameValue.setText( "SS:" + ss.name() + "Connected to " + wifiInfo.getSSID()+ String.format("%d",Rssi));
      mSSIDOrBdAddrValue.setText(wifiState);

      mWifiStateHandler.postDelayed(this, 1000);
    }
  };

  void printRPYT( final float roll, final float pitch, final float yaw, final float thrust, final short flightMode )
  {

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String rpyt = "";
        rpyt += String.format( "roll: %1.5f  ", roll );
        rpyt += String.format( "pitch: %1.5f  ", pitch );
        rpyt += String.format( "yaw: %1.5f  ", yaw );
        rpyt += String.format( "thrust: %1.5f  ", thrust );

        mRPYTLineLayout.setVisibility(View.VISIBLE);
        mRPYTString.setVisibility(View.VISIBLE);
        mRPYTString.setText(rpyt);

        mFlightModeLineLayout.setVisibility(View.VISIBLE);
        mFlightModeString.setVisibility(View.VISIBLE);
        mFlightModeString.setText("mode: " + mFlightModeMapping.getName(flightMode));
      }
    });
  }

  void printRPYTString( final String s )
  {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mRPYTString.setText(s);
        mRPYTLineLayout.setVisibility(View.VISIBLE);
        mRPYTString.setVisibility(View.VISIBLE);
      }
    });
  }

  private boolean wifiScan()
  {
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
      return false;
    }
    WifiManager mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);


    boolean ret = mWifiManager.isWifiEnabled();
    ret = ret || ((android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) &&
            mWifiManager.isScanAlwaysAvailable());
    return ret;
  }

  protected void onSaveInstanceState (Bundle outState) {
    Log.d ("GStreamer", "Saving state, playing:" + is_playing_desired);
    outState.putBoolean("playing", is_playing_desired);
  }

  protected void onDestroy() {
    if (!SAMSUNG) {
      mStreamHandler.finalizeSH();
    }
    super.onDestroy();
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {
    Log.d("GStreamer", "Surface changed to format " + format + " width "
            + width + " height " + height);

    mStreamHandler.surfaceInit(holder.getSurface());
  }

  public void surfaceCreated(SurfaceHolder holder) {
    Log.d("GStreamer", "Surface created: " + holder.getSurface());
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    Log.d("GStreamer", "Surface destroyed");
    //mStreamHandler.surfaceFinalize ();
  }

  protected void makeToast(String message, int backgroundId ) {
    // get your custom_toast.xml layout
    LayoutInflater inflater = getLayoutInflater();

    View layout = inflater.inflate(R.layout.toast,
            (ViewGroup) findViewById(R.id.custom_toast_layout_id));
    ImageView image = (ImageView) layout.findViewById(R.id.toast_image);
    image.setImageResource(backgroundId);

    // set a message
    TextView text = (TextView) layout.findViewById(R.id.text);
    text.setText(message);

    // Toast...
    Toast toast = new Toast(getApplicationContext());
    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 100);
    toast.setDuration(Toast.LENGTH_SHORT);
    toast.setView(layout);
    toast.show();
  }
}


