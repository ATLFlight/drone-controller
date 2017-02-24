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

package atlflight;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class MAVLinkUdpTransport implements MAVLinkTransport {
  public MAVLinkUdpTransport( int localPort ) {
    if ( localPort != 0 ) {
      mLocalUdpPort = localPort;
    }
  }

  public boolean open() {
    boolean ok = false;
    if (mDatagramChannel == null) {
      try {
        mDatagramChannel = DatagramChannel.open();
        mDatagramChannel.socket().setTrafficClass(0x30); // Corresponds to AC_VO 802.11d
        mDatagramChannel.socket().bind(new InetSocketAddress(mLocalUdpPort));
        ok = true;
      } catch (IOException e) {
      }
    }
    return ok;
  }

  public void close() {
    if (mDatagramChannel != null) {
      try {
        mDatagramChannel.close();
      } catch (IOException e) {
      }
      mDatagramChannel = null;
    }
  }

  public boolean connect(int localPort, InetSocketAddress remoteAddr) {
    boolean ok = false;
    if (mDatagramChannel != null) {
      mRemoteAddr = remoteAddr;
      ok = true;
    }
    return ok;
  }

  public boolean send( byte[] data ) {
    /*
    StringBuilder stringBuilder = new StringBuilder();
    for(byte b: data) {
      stringBuilder.append(String.format("%02X ", b));
    }
    //Log.e("UDP Transport", stringBuilder.toString());
    */
    boolean ok = false;
    if (mDatagramChannel != null) {
      try {
        mDatagramChannel.socket().send(new DatagramPacket(data,
                                                          data.length,
                                                          mRemoteAddr.getAddress(),
                                                          mRemoteAddr.getPort()));
        ok = true;
      } catch (IOException e) {
      }
    }
    return ok;
  }

  public boolean receive(ByteBuffer data ) {
    boolean ok = false;
    if (mDatagramChannel != null) {
      try {
        mDatagramChannel.receive(data);
        ok = true;
      } catch (IOException e) {
      }
    }
    return ok;
  }

  public void setRemoteAddr( InetSocketAddress addr ) {
    mRemoteAddr = addr;
  }

  private int               mLocalUdpPort = 14551;
  private InetSocketAddress mRemoteAddr;
  private DatagramChannel   mDatagramChannel;
}
