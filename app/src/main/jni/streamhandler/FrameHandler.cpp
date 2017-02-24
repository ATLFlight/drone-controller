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

#include <fstream>
#include "StreamHandler.hpp"
#include "FrameHandler.hpp"

FrameHandler& FrameHandler::getInstance()
{
	static FrameHandler singletonInstance;
	return singletonInstance;
}


bool FrameHandler::init()
{
	int rc = pthread_create(&m_thread_id,NULL, &(FrameHandler::decodeFrameHelper), this);
	if(rc)
	{
		__android_log_print (ANDROID_LOG_ERROR, "FrameHandler", "Unable to start thread %d\n", rc);
		return false;
	}

	return true;

}

void* FrameHandler::decodeFrame()
{
	//
	//__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","In %s\n", __PRETTY_FUNCTION__);

	while(1)
	{

		gboolean res = gst_app_sink_is_eos((GstAppSink *)pData->appsink);
		GstSample*    sample = NULL;
		if(res)
		{
			__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","EOS, exiting\n");
			//Sleep? or Exit?
			sleep(60);
		}
		else
		{
			//GST_DEBUG("blocked in pull_sample\n");
			sample = gst_app_sink_pull_sample((GstAppSink*)pData->appsink);

			if (sample)
			{
				extractFrame(gst_sample_get_buffer(sample));
			}
			else
			{
				__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","buffer returned null\n");
				usleep(100*1000);
			}
		}
	}

	return NULL;
}


bool FrameHandler::extractFrame(GstBuffer * buffer)
{
	//Call back

	JNIEnv *env = get_jni_env ();

	//
	const int dataSize = gst_buffer_get_size(buffer);

    GstMapInfo info;

	//To send the composite frame

	gst_buffer_map(buffer,&info,GST_MAP_READ);

	jbyteArray imageData = env->NewByteArray(dataSize);

	env->SetByteArrayRegion(imageData, 0, dataSize, (jbyte*)info.data);

	env->CallVoidMethod ( pData->app, get_on_new_frame_method_id(),imageData, dataSize);

	//To send a single frame, enable this

	if (env->ExceptionCheck ())
	{
		//__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","Failed to call java image data callback\n");
		env->ExceptionClear ();
	}

	gst_buffer_unref (buffer);

#if 0

   static int frameCount = 100;

   if(frameCount-- > 0)
   {
      std::ofstream frame;

      frame.open("/sdcard/frameraw.yuv",std::fstream::out|std::fstream::binary);

      //save the image
      frame.write((char*)info.data,dataSize);

      frame.close();

	  jbyte* ptr = env->GetByteArrayElements(imageData,NULL);

	  frame.open("/sdcard/frameba.yuv",std::fstream::out|std::fstream::binary);

	  //save the image
	  frame.write((char*)ptr,dataSize);

	  frame.close();

      //__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","In %s saved raw images\n", __PRETTY_FUNCTION__);
   }

#endif

	gst_buffer_unmap(buffer,&info);

	env->DeleteLocalRef(imageData);

	return true;
}

void* FrameHandler::decodeFrameHelper(void* param)
{
	//__android_log_print(ANDROID_LOG_DEBUG, "FrameHandler","In %s\n", __PRETTY_FUNCTION__);
	if(param==NULL)
	{
		__android_log_print(ANDROID_LOG_ERROR, "FrameHandler", "Invalid pointer\n");
		pthread_exit(NULL);
	}
	//Start the thread
	return ((FrameHandler*)param)->decodeFrame();
}

