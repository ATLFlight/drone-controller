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

/*
The MIT License (MIT)

Copyright (c) 2016 Shahzada Hatim Mushtaq
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
https://github.com/geoaxis/BluetoothTest/tree/master/app/src/main/java/com/bluetooth/test/bluetoothtest

 */

package atlflight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import atlflight.connectionlistener.ConnectionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MAVLinkBTLETransport implements MAVLinkTransport {

  private static String TAG = MAVLinkBTLETransport.class.getSimpleName();

  // To generate these UUIDs:
  // $ uuidgen
  private final UUID mPrimaryServUUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
  private final UUID mSDPUUID         = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
  private final UUID mTxDataCharUUID  = UUID.fromString("48d1ec47-1880-42fc-9cb8-124ffb0ad465");
  private final UUID mRxDataCharUUID  = UUID.fromString("ef188a2e-d3fd-4ed5-aa46-0f2c3d4e7a7a");

  private BluetoothManager   mManager;
  private BluetoothAdapter   mAdapter;
  private BluetoothDevice    mDevice;
  private String             mBdAddr;
  private boolean            mConnected;
  private ConnectionListener mConnectionListener;

  private Context                   mContext;
  private BluetoothLeAdvertiser     mAdvertiser;
  private BluetoothGattServer       mGattServer;
  private AdvertiseData.Builder     mAdvDataBuilder = new AdvertiseData.Builder();
  private AdvertiseSettings.Builder mAdvSettingsBuilder = new AdvertiseSettings.Builder();
  private boolean                   mAdvertising = false;
  private Semaphore                 mRxDataReady = new Semaphore(0);
  private Random                    mRandom = new Random();
  private Semaphore                 mLeConnected = new Semaphore(0);

  private BluetoothGattService mPrimaryService =
      new BluetoothGattService(mPrimaryServUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

  private BluetoothGattCharacteristic mTxDataChar =
      new BluetoothGattCharacteristic(mTxDataCharUUID,
                                      BluetoothGattCharacteristic.PROPERTY_READ |
                                          BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                                      BluetoothGattCharacteristic.PERMISSION_READ);

  private BluetoothGattCharacteristic mRxDataChar =
      new BluetoothGattCharacteristic(mRxDataCharUUID,
                                      BluetoothGattCharacteristic.PROPERTY_WRITE,
                                      BluetoothGattCharacteristic.PERMISSION_WRITE);

  private Thread mConnectorThread;

  private ArrayList<BluetoothGattService> mGattServices = new ArrayList<BluetoothGattService>();

  public MAVLinkBTLETransport(Context context) {
    mContext = context;
    mManager = (BluetoothManager)
        mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);

    mAdapter = BluetoothAdapter.getDefaultAdapter();

    if ((mAdvertiser=mAdapter.getBluetoothLeAdvertiser()) != null) {
      mTxDataChar.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
      mRxDataChar.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
      mPrimaryService.addCharacteristic(mRxDataChar);
      mPrimaryService.addCharacteristic(mTxDataChar);

      mGattServices.add(mPrimaryService);

      mAdvDataBuilder.addServiceUuid(new ParcelUuid(mPrimaryService.getUuid()));

      mAdvDataBuilder.setIncludeTxPowerLevel(false);
      mAdvDataBuilder.setIncludeDeviceName(true);
      mAdvSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
      mAdvSettingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
      mAdvSettingsBuilder.setConnectable(true);
    }
  }

  public boolean open() {
    boolean ok = false;
    if (mAdvertiser != null) {
      if (mGattServer == null) {
        mAdapter.setName(genRandomBdaddr());
        mGattServer = mManager.openGattServer(mContext, mGattServerCallback);
        for (int i = 0; i < mGattServices.size(); i++) {
          BluetoothGattService gs = mGattServices.get(i);
          // Only want one copy of our service being advertised in the global gatt server
          // so if the service already exists, remove it before adding it.
          if (mGattServer.getService(gs.getUuid()) != null) {
            mGattServer.removeService(gs);
          }
          mGattServer.addService(gs);
        }
        ok = true;
      }
    } else {
      Toast.makeText(mContext, "LE peripheral mode not supported", Toast.LENGTH_SHORT).show();
      return true;
    }
    return ok;
  }
  
  public void close() {
    if (mConnectorThread != null) {
      mConnectorThread.interrupt();
      try {
        mConnectorThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      mConnectorThread = null;
    }

    // Close and null out the mGattServer
    // object so that when the onConnectionStateChange fires
    // for the disconnect, we don't start up the connecting
    // thread.
    if (mGattServer != null) {
      for (int i = 0; i < mGattServices.size(); i++) {
        mGattServer.removeService(mGattServices.get(i));
      }
      mGattServer.close();
      mGattServer = null;
    }

    mDevice = null;
  }

  public boolean connect(String bdAddr, ConnectionListener listener) {

    if (isBdAddrValid(bdAddr) && !mConnected && mConnectorThread == null) {

      mBdAddr = bdAddr;
      mConnectionListener = listener;

      mConnectorThread = new Thread() {
        @Override
        public void run() {
          BluetoothDevice device = mAdapter.getRemoteDevice(mBdAddr.toUpperCase());

          ByteBuffer rsp = ByteBuffer.allocate(80);
          byte[] rspBA = rsp.array();

          while (true) {

            BluetoothSocket btSock = null;

            try {
              if (mConnectionListener != null) {
                mConnectionListener.onStateChange(mBdAddr, ConnectionListener.ConnectionState.CONNECTING);
              }

              mAdapter.cancelDiscovery();

              btSock = device.createRfcommSocketToServiceRecord(mSDPUUID);
              btSock.connect();

              OutputStream os = btSock.getOutputStream();
              InputStream is = btSock.getInputStream();
              os.write(("lecrr " + mAdapter.getName()).getBytes());
              int rspLen = is.read(rspBA, 0, rspBA.length);
              try {
                btSock.close();
              } catch (IOException e) {
              }
              if (rspLen > 0) {
                String s = new String(rspBA, "UTF-8");
                if (s.startsWith("lecr-ack")) {
                  enAdvertisement(true);
                  try {
                    if (mLeConnected.tryAcquire(10, TimeUnit.SECONDS)) {
                      Log.d(TAG, "LE Connected");
                    } else {
                      continue;
                    }
                  } catch (InterruptedException e) {
                    continue;
                  }
                }
              }
              break;
            }
            catch (IOException e1) {
              if (btSock != null) {
                try {
                  btSock.close();
                } catch (IOException e2) {
                }
              }
            }

            try {
              Thread.sleep(1000);
            }
            catch (InterruptedException inte) {
              break;
            }
          }

          Log.d(TAG, "Connection Thread exiting...");
          mConnectorThread = null;
        }
      };
      mConnectorThread.start();
      return true;
    }
    return false;
  }

  private String genRandomBdaddr() {
    byte[] b = new byte[6];
    mRandom.nextBytes(b);
    return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                         b[0],b[1],b[2],b[3],b[4],b[5]);
  }

  public static boolean isBdAddrValid(String bdAddr) {
    boolean ok = false;
    if (bdAddr != null) {
      ok = Pattern.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", bdAddr);
    }
    return ok;
  }

  public boolean send(byte[] data) {
    if (mConnected) {
      mTxDataChar.setValue(data);
      mGattServer.notifyCharacteristicChanged(mDevice, mTxDataChar, false);
      return true;
    }
    return false;
  }

  public boolean receive(ByteBuffer data) {
    boolean ok = false;
    if (mConnected) {
      try {
        mRxDataReady.acquire();
        data.clear();
        data.put(mRxDataChar.getValue());
        //Log.d(TAG, "recv'd: " + data.position() + " bytes");
        ok = true;
      } catch (InterruptedException e) {
      }
    }
    return ok;
  }

  private void enAdvertisement(boolean en) {
    if (en != mAdvertising) {

      if (en) {
        Log.d(TAG, "Starting adv");
        mAdvertiser.startAdvertising(mAdvSettingsBuilder.build(),
                                     mAdvDataBuilder.build(),
                                     mAdvertiseCallback);
      } else {
        Log.d(TAG,"Stopping adv");
        mAdvertiser.stopAdvertising(mAdvertiseCallback);
        mAdvertising = false;
      }
    }
  }

  private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings advertiseSettings) {
      mAdvertising = true;
      Log.d(TAG, "Advertisement success");
    }

    @Override
    public void onStartFailure(int i) {
      Log.e(TAG, "Advertisement failure: " + i);
    }
  };

  public BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onConnectionStateChange(final BluetoothDevice device, int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      if (status != 0) {
        return;
      }

      switch (newState) {
      case BluetoothProfile.STATE_CONNECTED:
        Log.d(TAG, "mGattServerCallback.Connected");
        enAdvertisement(false);
        mConnected = true;
        mDevice    = device;
        mLeConnected.release();
        if (mConnectionListener != null) {
          mConnectionListener.onStateChange(mDevice.getAddress(), ConnectionListener.ConnectionState.CONNECTED);
        }
        break;
      case BluetoothProfile.STATE_DISCONNECTED:
        Log.d(TAG, "mGattServerCallback.Disconnected");
        synchronized (this) {
          mConnected = false;
          mDevice = null;
        }
        if (mConnectionListener != null) {
          mConnectionListener.onStateChange(mBdAddr, ConnectionListener.ConnectionState.DISCONNECTED);
        }

        // If the GattServer object isn't NULL it means this was remote
        // initiated disconnect. In this case, we go back to the initial
        // connecting state by starting up the connecting thread.
        if (mGattServer != null) {
          connect(mBdAddr, mConnectionListener);
        }
        break;
      }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
      super.onServiceAdded(status, service);
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device,
                                            int requestId,
                                            int offset,
                                            BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

      byte[] retVal = null;

      if (characteristic.getUuid().equals(mTxDataChar.getUuid())) {
        retVal = mTxDataChar.getValue();
      } else if (characteristic.getUuid().equals(mRxDataChar.getUuid())) {
        retVal = mRxDataChar.getValue();
      }

      mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, retVal);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
      super.onCharacteristicWriteRequest(device, requestId, characteristic,
              preparedWrite, responseNeeded,
              offset, value);

      if (mRxDataChar.getUuid().equals(characteristic.getUuid())) {
        mRxDataChar.setValue(value);
        mRxDataReady.release();
      }

      if (responseNeeded) {
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
      }
    }
  };
}
