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

import android.util.Log;

import atlflight.threadwrapper.ThreadWrapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MAVLinkMessenger {

  private static String TAG = MAVLinkMessenger.class.getSimpleName();

  private ThreadWrapper mReceiverThread;
  private ThreadWrapper mSenderThread;

  private Semaphore mByteArrayQueueSem = new Semaphore(1);
  private ConcurrentLinkedQueue<byte[]> mHiPriByteArrayQueue = new ConcurrentLinkedQueue<byte[]>();
  private ConcurrentLinkedQueue<byte[]> mLoPriByteArrayQueue = new ConcurrentLinkedQueue<byte[]>();

  private ConcurrentHashMap<Integer, MsgHandler> mMessageHandlers =
      new ConcurrentHashMap<Integer, MsgHandler>();

  private MAVLinkTransport mMAVLinkTransport;
  
  public static class MsgHandler {
    private ConcurrentLinkedQueue<byte[]> mMsgQueue = null;
    private Semaphore mMsgQueueSem = null;

    public MsgHandler() {
      mMsgQueue = new ConcurrentLinkedQueue<byte[]>();
      mMsgQueueSem = new Semaphore(0);
    }

    public void enqMsg(byte[] m) {
      mMsgQueue.add(m);
      mMsgQueueSem.release();
    }

    public byte[] deqMsg(int timeout, TimeUnit tu) throws InterruptedException {
      byte[] msg = null;
      if (!mMsgQueue.isEmpty()) {
        msg = mMsgQueue.poll();
      }
      else {
        // Wait until sem released, timeout occurs, or thread
        // interrupted
        mMsgQueueSem.tryAcquire(timeout, tu);
        if (!mMsgQueue.isEmpty()) {
          msg = mMsgQueue.poll();
        }
      }
      return msg;
    }

    public byte[] deqMsg() throws InterruptedException {
      byte[] msg = null;
      if (!mMsgQueue.isEmpty()) {
        msg = mMsgQueue.poll();
      }
      else {
        // Wait until sem is released or thread
        // interrupted
        mMsgQueueSem.acquire();
        if (!mMsgQueue.isEmpty()) {
          msg = mMsgQueue.poll();
        }
      }
      return msg;
    }
  }

  private void startThreads() {
    // Sender thread is responsible for sending queued up messages to the
    // drone.

    if (mSenderThread == null) {
      mSenderThread = new ThreadWrapper() {
        @Override
        public void run() {
          while (!mQuit) {
            // Wait for something to arrive...
            try {
              mByteArrayQueueSem.tryAcquire(500, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
              continue;
            }

            // Empty the queues. Only empty the lo pri queue once the hi pri queue has
            // been fully emptied.
            while (!mQuit) {
              useByteArray();
            }
          }
          Log.d(TAG, "Sender thread exited");
        }
      };
      mSenderThread.start();
    }
  }

  private void useByteArray() {
    byte[] msg;

    if (!mHiPriByteArrayQueue.isEmpty()) {
      msg = mHiPriByteArrayQueue.poll();
    }
    else if (!mLoPriByteArrayQueue.isEmpty()) {
      msg = mLoPriByteArrayQueue.poll();
    }
    else {
      return;
    }
    mMAVLinkTransport.send(msg);
  }

  private void stopThreads() {
    if (mReceiverThread != null) {
      mReceiverThread.quit();
      mReceiverThread = null;
    }

    if (mSenderThread != null) {
      mSenderThread.quit();
      mSenderThread = null;
    }
  }

  public boolean send(byte[] mavlinkMessageArray, boolean hiPri) {
    boolean ok = false;
    if (mMAVLinkTransport != null) {
      // Queue the message into the selected queue
      if (hiPri) {
        if (!mHiPriByteArrayQueue.add(mavlinkMessageArray)) {
          Log.e(TAG, "Failed to enqueue hi pri byte array message");
        }
      }
      else {
        if (!mLoPriByteArrayQueue.add(mavlinkMessageArray)) {
          Log.e(TAG, "Failed to enqueue lo pri byte array message");
        }
      }
      mByteArrayQueueSem.release();
      ok = true;
    }
    return ok;
  }

  public boolean attach(MAVLinkTransport transport) {
    boolean ok = false;
    if (mMAVLinkTransport == null) {
      mMAVLinkTransport = transport;
      startThreads();
      ok = true;
    }
    return ok;

  }
  public void detach() {
    stopThreads();
    mMAVLinkTransport = null;
  }

  public void registerMessageHandler(int msgId, MsgHandler h) {
    if (mMessageHandlers.containsKey(msgId)) {
      mMessageHandlers.remove(msgId);
    }
    if (h != null) {
      mMessageHandlers.put(msgId, h);
    }
  }
}
