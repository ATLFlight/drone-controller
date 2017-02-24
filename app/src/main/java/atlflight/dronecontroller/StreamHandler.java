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

import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.freedesktop.gstreamer.GStreamer;

public class StreamHandler {

    private static final String TAG = StreamHandler.class.getCanonicalName();
    private native boolean nativeInit( String imageURI, int bufferSize,boolean enable_app_sink, boolean enable_video_sink);     // Initialize
    //private native void nativeInit();     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();
    private long native_custom_data;      // Native code will use this to keep private data

    private int m_BufferSize;

    private Context context;
    private Activity activity;

    private final Semaphore m_FrameAvailable = new Semaphore(0, true);


    //This is called from the native jni context
    //
    public void processFrame(byte[]data, int size)
    {
        //copy the buffer and signal the callback calling thread

        /*
        Log.d(TAG,"Got " + size +" bytes");

        synchronized(m_ImageData)
        {
            m_ImageData.clear();
            m_ImageData.append(data, 0, size);
        }

        m_FrameAvailable.release();
        */
    }

    public StreamHandler(Activity mainactivity, Context maincontext ) {
        activity = mainactivity;
        context = maincontext;
    }

    //boolean init(){
    public boolean init(Context context, String imageURI, int bufferSize,  Object userData) {

        Log.d(TAG,"Init Called");

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context);
        } catch (Exception e) {

            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        //Allocate the image buffer

        m_BufferSize = bufferSize;

        //app sink
        //return (nativeInit( imageURI, bufferSize,true,false));

        //Video sink
        return (nativeInit( imageURI, bufferSize,false,true));

        //Test
        //return (nativeInit( imageURI, bufferSize,true,true));
    }

    public void finalizeSH() {
        Log.d(TAG,"Finalize Called");
        nativeFinalize();
    }

    public void play() {
        Log.d(TAG,"Play Called");
        nativePlay();
    }

    public void pause() {
        Log.d(TAG,"Pause Called");
        nativePause();
    }

    public void surfaceInit(Object surface) {
        Log.d(TAG,"SurfaceInit Called");
        nativeSurfaceInit(surface);
    }

    public void surfaceFinalize() {
        Log.d(TAG,"SurfaceFinalize Called");
        nativeSurfaceFinalize();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {

        Log.d(TAG,"Message: " + message);
        //      activity.runOnUiThread (new Runnable() {
        //        public void run() {
        //          tv.setText(message);
        //        }
        //      });
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        Log.i (TAG, "Gst initialized. Restoring state, playing:" + "true");

        play();

       /* activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.findViewById(R.id.button_play).setEnabled(true);
                activity.findViewById(R.id.button_stop).setEnabled(true);
            }
        });*/

        play();

        // Restore previous playing state
        //      if (is_playing_desired) {
        //        play();
        //      } else {
        //        pause();
        //      }

        // Re-enable buttons, now that GStreamer is initialized
        //      final Activity activity = this;
        //      runOnUiThread(new Runnable() {
        //          public void run() {
        //              activity.findViewById(R.id.button_play).setEnabled(true);
        //              activity.findViewById(R.id.button_stop).setEnabled(true);
        //          }
        //      });
    }


    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("streamhandler");
        nativeClassInit();
    }

}
