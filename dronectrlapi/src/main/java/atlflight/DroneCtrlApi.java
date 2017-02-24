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

package atlflight.dronectrlapi;

import android.content.Context;
import android.util.Log;

import atlflight.MAVLinkBTLETransport;
import atlflight.MAVLinkMessenger;
import atlflight.MAVLinkTransport;
import atlflight.MAVLinkUdpTransport;
import atlflight.MavlinkWrapper;
import atlflight.threadwrapper.ThreadWrapper;
import atlflight.connectionlistener.ConnectionListener;

import java.net.InetSocketAddress;

public abstract class DroneCtrlApi {

    private static final String TAG = "DroneControllerAPI";

    public class DroneVersionInfo {
        public int mModel;
        public int mSerialNumber;
        public byte[] mFirmwareVersion = new byte[ 3 ]; // [2]: Major, [1]: Minor, [0]: Patch
    }

    // Interface for forwarding drone heartbeat messages
    // to the app.
    public interface HeartbeatReportListener {
        void notify(HeartbeatReport hbr);
    }

    // Interface for forwarding drone Battery Status messages to the app.
    public interface BatteryStatusListener {
        void notify(BatteryStatus batteryStatus);
    }

    // Contains everything we need to control the
    // drone's attitude and control the desired mode
    public static class Setpoint {
        // Required normalized range: -1.0..1.0
        public float mRoll;
        public float mPitch;
        public float mYaw;

        // Required range: 0..1.0
        public float mThrust;

        public short mFlightModeSwitch;
    }

    // PX4 uses the CommandLong for control messages
    // http://mavlink.org/messages/common#COMMAND_LONG
    public static class CommandLong {
        public byte target_system = 1;
        public byte target_component = 0;
        public short command;
        public byte confirmation; // Set to 1 to send a confirmation of the command to the GCS
        public int param1;
        public int param2;
        public int param3;
        public int param4;
        public int param5;
        public int param6;
        public int param7;
    }

    // Drone battery status message report definition.
    // https://pixhawk.ethz.ch/mavlink/#BATTERY_STATUS
    public static class BatteryStatus {
        public int mId;
        public int mBatteryFunction;
        public int mType;
        public int mTemperature;
        public short[] mVoltages;
        public int mCurrentBattery;
        public int mCurrentConsumed;
        public int mEnergyConsumed;
        public int mBatteryRemaining;

        public BatteryStatus() {
            mId = -1;
            mBatteryFunction = -1;
            mType = -1;
            mTemperature = -1;
            mVoltages = new short[ 10 ];
            mCurrentBattery = -1;
            mCurrentConsumed = -1;
            mEnergyConsumed = -1;
            mBatteryRemaining = -1;
        }
    }

    // Drone heartbeat message report definition.
    public static class HeartbeatReport {
        public int mCustomMode;
        public int mType;
        public int mAutopilot;
        public int mBaseMode;
        public int mSystemStatus;
        public int mMAVLinkVersion;

        public HeartbeatReport() {
            mCustomMode = -1;
            mType = -1;
            mAutopilot = -1;
            mBaseMode = -1;
            mSystemStatus = -1;
            mMAVLinkVersion = -1;
        }
    }

    //private static final String LOG_TAG = DroneCtrlApi.class.getSimpleName();

    private MAVLinkMessenger mMAVLinkMessenger;
    private ThreadWrapper mSetpointTxThread;
    private ThreadWrapper mCommandLongTxThread;
    private int mSetpointTxIntervalMs = 20;
    private int mCommandLongTxIntervalMs = 100;
    private boolean mConnected;
    private MAVLinkTransport mMavLinkTransport;
    private ConnectionListener mConnectionListener;

    private ConnectionListener mBTLEConnectionListener = new ConnectionListener() {
        @Override
        public void onStateChange(final String deviceAddr, ConnectionState newState) {
            switch (newState) {
                case CONNECTED:
                    startSetpoinTxThread();
                    break;
                case CONNECTING:
                    break;
                case DISCONNECTED:
                    stopThread();
                    break;
            }
            if (mConnectionListener != null) {
                mConnectionListener.onStateChange(deviceAddr, newState);
            }
        }
    };

    public DroneCtrlApi() {
        // Thread to tx the manual setpoint message containing the RPYT values.
        // Calls the getSetpoint method to actually retrieve the values from the
        // app.

        mMAVLinkMessenger = new MAVLinkMessenger();
    }

    private void startSetpoinTxThread() {
        if (mSetpointTxThread == null) {
            mSetpointTxThread = new ThreadWrapper() {
                @Override
                public void run() {
                    while (!mQuit) {
                        sendSetpoint(getName());
                        sleep(mSetpointTxIntervalMs);
                    }
                }
            };
            mSetpointTxThread.start();
        }
    }

    public void startCommandLongTxThread() {
        if (mCommandLongTxThread == null) {
            mCommandLongTxThread = new ThreadWrapper() {
                @Override
                public void run() {
                    sendCommandLong(getName());
                    sleep(mCommandLongTxIntervalMs);
                    mCommandLongTxThread = null;
                    quit();
                }
            };
            mCommandLongTxThread.start();
        }
    }

    private void sendCommandLong(String threadName) {
        CommandLong cmdLong = getCommandLong();
        byte[] commandLong =  new byte[7];
        if (cmdLong != null) {
            MavlinkWrapper mavlinkWrapper = new MavlinkWrapper();

            commandLong = mavlinkWrapper.mavlink_build_command_long(
                    (short) (cmdLong.command),
                    (byte) (cmdLong.confirmation),
                    (float) (cmdLong.param1),
                    (float) (cmdLong.param2),
                    (float) (cmdLong.param3),
                    (float) (cmdLong.param4),
                    (float) (cmdLong.param5),
                    (float) (cmdLong.param6),
                    (float) (cmdLong.param7));
            Log.e(threadName, "param1: " + Integer.toString(cmdLong.param1));
            Log.e(threadName, "param2: " + Integer.toString(cmdLong.param2));
        } else {
            Log.d(threadName, "command long is null");
        }
        mMAVLinkMessenger.send(commandLong, true);
    }

    private void sendSetpoint(String threadName) {
        Setpoint sp = getSetpoint();
        byte[] setpoint = new byte[7];
        if (sp != null) {
            MavlinkWrapper mavlinkWrapper = new MavlinkWrapper();

            setpoint = mavlinkWrapper.mavlink_build_manual_control(
                (byte)(0),
                (short)(sp.mPitch * 1000),
                (short)(sp.mRoll * 1000),
                (short)(sp.mThrust * 1000),
                (short)(sp.mYaw * 1000),
                (short)(sp.mFlightModeSwitch));
        } else {
            Log.d(threadName, "manual control is null");
        }
        mMAVLinkMessenger.send(setpoint, true);
    }

    private void stopThread() {
        if (mSetpointTxThread != null) {
            mSetpointTxThread.quit();
            mSetpointTxThread = null;
        }

        if (mCommandLongTxThread != null) {
            mCommandLongTxThread.quit();
            mCommandLongTxThread = null;
        }
    }

    // These methods must be defined by the app.
    public abstract Setpoint getSetpoint();

    public abstract CommandLong getCommandLong();

    // Set's the desired manual setpoint tx rate
    public void setTxInterval(int intervalMs) {
        mSetpointTxIntervalMs = intervalMs;
    }

    // Starts everything up
    public boolean connect(int port, InetSocketAddress remoteAddr) {
        boolean connected = false;
        if (!mConnected) {
            Log.e(TAG, "!mConnected");
            MAVLinkUdpTransport udpTransport = new MAVLinkUdpTransport(port);
            if (udpTransport.open() &&
                udpTransport.connect(port, remoteAddr) &&
                mMAVLinkMessenger.attach(udpTransport)) {
                mConnected = true;
                mMavLinkTransport = udpTransport;
                startSetpoinTxThread();
            }
            connected = mConnected;
        }
        return connected;
    }

    // Starts everything up
    public boolean connect(Context context, String bdaddr, ConnectionListener listener) {
        boolean ok = false;
        if (!mConnected) {
            mConnectionListener = listener;
            MAVLinkBTLETransport btleTransport = new MAVLinkBTLETransport(context);
            if (btleTransport.open() &&
                btleTransport.connect(bdaddr, mBTLEConnectionListener) &&
                mMAVLinkMessenger.attach(btleTransport)) {
                mConnected = true;
                ok = true;
                mMavLinkTransport = btleTransport;
                startSetpoinTxThread();
            } else {
                mConnectionListener = null;
            }
        }
        return ok;
    }

    // Shuts everything down
    public void disconnect() {
        if (mConnected) {
            mMAVLinkMessenger.detach();
            mMavLinkTransport.close();
            stopThread();
            mConnectionListener = null;
            mMavLinkTransport = null;
            mConnected = false;
        }
    }

    public boolean isConnected() {
        return mConnected;
    }
}
