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

#include <string.h>
#include <unistd.h>
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <gst/gst.h>

#include <gst/video/videooverlay.h>
#include <gst/video/video.h>
#include <gst/app/gstappsink.h>

#include <pthread.h>

#include <string>
#include "StreamHandler.hpp"
#include "FrameHandler.hpp"

GST_DEBUG_CATEGORY_STATIC (debug_category);
#define GST_CAT_DEFAULT debug_category

/* These global variables cache values which are not changing during execution */
static pthread_t gst_app_thread;
static pthread_key_t current_jni_env;
static JavaVM *java_vm;
static jfieldID custom_data_field_id;
static jmethodID set_message_method_id;
static jmethodID on_gstreamer_initialized_method_id;
static jmethodID on_new_frame_method_id;

CustomData *pData = NULL;

jmethodID get_on_new_frame_method_id()
{
	return on_new_frame_method_id;
}


/*
 * Private methods
 */

/* Register this thread with the VM */
static JNIEnv *attach_current_thread (void)
{
	JNIEnv *env;
	JavaVMAttachArgs args;

	GST_DEBUG ("Attaching thread %p", g_thread_self ());
	args.version = JNI_VERSION_1_4;
	args.name = NULL;
	args.group = NULL;

	if (java_vm->AttachCurrentThread (&env, &args) < 0)
	{
		GST_ERROR ("Failed to attach current thread");
		return NULL;
	}

	return env;
}

/* Unregister this thread from the VM */
static void detach_current_thread (void *env)
{
	GST_DEBUG ("Detaching thread %p", g_thread_self ());
	java_vm->DetachCurrentThread ();
}

/* Retrieve the JNI environment for this thread */
JNIEnv *get_jni_env (void)
{
	JNIEnv *env;

	if ((env = (JNIEnv*) pthread_getspecific (current_jni_env)) == NULL)
	{
		env = attach_current_thread ();
		pthread_setspecific (current_jni_env, env);
	}

	return env;
}


/* Change the content of the UI's TextView */
static void set_ui_message (const gchar *message, CustomData *data)
{
	//JNIEnv *env = get_jni_env ();
	GST_DEBUG ("Setting message to: %s", message);
	//  jstring jmessage = env->NewStringUTF( message);
	//  env->CallVoidMethod ( data->app, set_message_method_id, jmessage);
	//  if (env->ExceptionCheck ()) {
	//    GST_ERROR ("Failed to call Java method");
	//    env->ExceptionClear ();
	//  }
	//  env->DeleteLocalRef ( jmessage);
}

/* Retrieve errors from the bus and show them on the UI */
static void error_cb (GstBus *bus, GstMessage *msg, CustomData *data)
{
	GError *err;
	gchar *debug_info;
	gchar *message_string;

	gst_message_parse_error (msg, &err, &debug_info);
	message_string = g_strdup_printf ("Error received from element %s: %s", GST_OBJECT_NAME (msg->src), err->message);
	g_clear_error (&err);
	g_free (debug_info);
	set_ui_message (message_string, data);
	g_free (message_string);
	gst_element_set_state (data->pipeline, GST_STATE_NULL);
}

/* Notify UI about pipeline state changes */
static void state_changed_cb (GstBus *bus, GstMessage *msg, CustomData *data)
{
	GstState old_state, new_state, pending_state;
	gst_message_parse_state_changed (msg, &old_state, &new_state, &pending_state);
	/* Only pay attention to messages coming from the pipeline, not its children */
	if (GST_MESSAGE_SRC (msg) == GST_OBJECT (data->pipeline)) {
		gchar *message = g_strdup_printf("State changed to %s", gst_element_state_get_name(new_state));
		set_ui_message(message, data);
		g_free (message);
	}
}

/* Check if all conditions are met to report GStreamer as initialized.
 * These conditions will change depending on the application */
static void check_initialization_complete (CustomData *data)
{
	JNIEnv *env = get_jni_env ();

	if(data->enable_app_sink)
	{
		if(!data->initialized && data->main_loop)
		{
			data->initialized = TRUE;
			//Call play
			gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
		}
	}
	else if(data->enable_video_sink)
	{
		if(!data->initialized && data->main_loop && data->native_window)
		{
			GST_DEBUG ("Initialization complete, notifying application. native_window:%p main_loop:%p", data->native_window, data->main_loop);

			/* The main loop is running and we received a native window, inform the sink about it */
			gst_video_overlay_set_window_handle (GST_VIDEO_OVERLAY(data->video_sink), (guintptr)data->native_window);

    		//gst_x_overlay_set_window_handle (GST_X_OVERLAY (data->video_sink), (guintptr)data->native_window);

			env->CallVoidMethod ( data->app, on_gstreamer_initialized_method_id);
			if (env->ExceptionCheck ())
			{
				GST_ERROR ("Failed to call Java method");
				env->ExceptionClear ();
			}
			data->initialized = TRUE;

			//Call play
			gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
		}
	}

}

/* Main method for the native code. This is executed on its own thread. */
static void *app_function (void *userdata)
{
	JavaVMAttachArgs args;
	GstBus *bus;
	CustomData *data = (CustomData *)userdata;
	GSource *bus_source;
	GError *error = NULL;

	GST_DEBUG ("Creating pipeline in CustomData at %p", data);

	/* Create our own GLib Main Context and make it the default one */
	data->context = g_main_context_new ();
	g_main_context_push_thread_default(data->context);

	/* Build pipeline */

	std::string launch_cmd;

	if(data->enable_app_sink)
	{
		launch_cmd  = "rtspsrc location=" + data->uri + " latency=95 drop-on-latency=true do-lost=true ! rtph264depay ! decodebin ! queue ! videoconvert ! video/x-raw,format=NV21 ! appsink name=sink sync=false";
	}
	else if(data->enable_video_sink)
	{
		//launch_cmd = "rtspsrc location=" + data->uri + " latency=95 ! rtpjpegdepay ! jpegdec ! queue ! autovideosink";
		launch_cmd = "rtspsrc location=" + data->uri + " latency=95 drop-on-latency=true do-lost=true ! rtph264depay ! decodebin ! queue ! videoconvert ! video/x-raw,format=NV21 ! autovideosink";
		// gstreamer command that tells gstreamer
		// latency=95: The maximum latency of the jitterbuffer. Packets will be kept in the buffer for at most this time. Probably milliseconds.
		// drop-on-latency=true : Drops oldest buffers when queue is completely filled.
		// do-lost=true: Sends message downstream (to the drone we think) when a packet is considered lost.
		// rtph264depay: whatever you got, decodes it as h264. (i.e., treats INPUT as h264). Extracts H264 video from RTP packets (RFC 3984)
		// decodebin:  constructs a decoding pipeline using available decoders and demuxers via auto-plugging
		// queue : a buffer
		// videoconvert: just a generic conversion, input to output. auto-detects input type (in this case h264 from decodebin) and outputs whatever the sink is connected to (in this case, video/x-raw, whose format is NV21)
		// autovideosink: a video sink that automatically detects an appropriate video sink to use.
	}

	GST_DEBUG("Launch cmd: %s\n", launch_cmd.c_str());

	data->pipeline = gst_parse_launch(launch_cmd.c_str(),&error);

	if (error)
	{
		gchar *message = g_strdup_printf("Unable to build pipeline: %s", error->message);
		g_clear_error (&error);
		set_ui_message(message, data);
		g_free (message);
		return NULL;
	}

	if(data->enable_app_sink)
	{
		data->appsink = gst_bin_get_by_name (GST_BIN (data->pipeline), "sink");
		gst_app_sink_set_max_buffers((GstAppSink *)data->appsink, 10);
		gst_app_sink_set_drop((GstAppSink *)data->appsink, TRUE);

		//gst_x_overlay_expose(GST_X_OVERLAY (data->appsink));
		//gst_x_overlay_expose(GST_X_OVERLAY (data->appsink));

		/* Set the pipeline to READY, so it can already accept a window handle, if we have one */
		gst_element_set_state(data->pipeline, GST_STATE_READY);

	}
	else if(data->enable_video_sink)
	{
		/* Set the pipeline to READY, so it can already accept a window handle, if we have one */
		gst_element_set_state(data->pipeline, GST_STATE_READY);

		data->video_sink = gst_bin_get_by_interface(GST_BIN(data->pipeline), GST_TYPE_VIDEO_OVERLAY);
		if (!data->video_sink)
		{
			GST_ERROR ("Could not retrieve video sink");
			return NULL;
		}
	}

	/* Instruct the bus to emit signals for each received message, and connect to the interesting signals */
	bus = gst_element_get_bus (data->pipeline);
	bus_source = gst_bus_create_watch (bus);
	g_source_set_callback (bus_source, (GSourceFunc) gst_bus_async_signal_func, NULL, NULL);
	g_source_attach (bus_source, data->context);
	g_source_unref (bus_source);
	g_signal_connect (G_OBJECT (bus), "message::error", (GCallback)error_cb, data);
	g_signal_connect (G_OBJECT (bus), "message::state-changed", (GCallback)state_changed_cb, data);
	gst_object_unref (bus);


	//Before entering glib main loop, create a thread to grab raw frames

	if(data->enable_app_sink)
	{
		FrameHandler& FHInst = FrameHandler::getInstance();

		bool retVal = FHInst.init();

		if(retVal == false)
		{
			__android_log_print(ANDROID_LOG_ERROR,"FrameHandler","Unable to initialize FrameHandler\n");
		}
	}

	/* Create a GLib Main Loop and set it to run */
	GST_DEBUG ("Entering main loop... (CustomData:%p)", data);
	data->main_loop = g_main_loop_new (data->context, FALSE);
	check_initialization_complete (data);
	g_main_loop_run (data->main_loop);
	GST_DEBUG ("Exited main loop");
	g_main_loop_unref (data->main_loop);
	data->main_loop = NULL;

	/* Free resources */
	g_main_context_pop_thread_default(data->context);
	g_main_context_unref (data->context);
	gst_element_set_state (data->pipeline, GST_STATE_NULL);
	gst_object_unref (data->video_sink);
	gst_object_unref(data->appsink);
	gst_object_unref (data->pipeline);

	return NULL;
}

/*
 * Java Bindings
 */

/* Instruct the native code to create its internal data structure, pipeline and thread */
static jboolean gst_native_init (JNIEnv* env, jobject thiz, jstring uri, jint size, jboolean enable_app_sink, jboolean enable_video_sink )
{
	__android_log_print(ANDROID_LOG_ERROR, "Streamer","In native init\n");
	pData = g_new0 (CustomData, 1);

	SET_CUSTOM_DATA ( thiz, custom_data_field_id, pData);
	GST_DEBUG_CATEGORY_INIT (debug_category, "StreamHandler", 0, "StreamHandler");
	gst_debug_set_threshold_for_name("StreamHandler", GST_LEVEL_DEBUG);
	GST_DEBUG ("Created CustomData at %p", pData);
	pData->app = env->NewGlobalRef ( thiz);

	//Set the size and uri

	//Extract the string and assign to the local string
	const char* tempStr = env->GetStringUTFChars(uri,NULL);
	//
	pData->uri.assign(tempStr);
	env->ReleaseStringUTFChars(uri,tempStr);

	pData->bufferSize = size;
	pData->enable_app_sink = enable_app_sink;
	pData->enable_video_sink = enable_video_sink;

	//__android_log_print(ANDROID_LOG_ERROR, "Streamer","uri %s size %d", pData->uri.c_str(), pData->bufferSize);

	GST_DEBUG ("Created GlobalRef for app object at %p", pData->app);
	pthread_create (&gst_app_thread, NULL, &app_function, pData);

	return true;
}

/* Quit the main loop, remove the native thread and free resources */
static void gst_native_finalize (JNIEnv* env, jobject thiz)
{
	CustomData *data = GET_CUSTOM_DATA ( thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Quitting main loop...");
	g_main_loop_quit (data->main_loop);
	GST_DEBUG ("Waiting for thread to finish...");
	pthread_join (gst_app_thread, NULL);
	GST_DEBUG ("Deleting GlobalRef for app object at %p", data->app);
	env->DeleteGlobalRef ( data->app);
	GST_DEBUG ("Freeing CustomData at %p", data);
	g_free (data);
	SET_CUSTOM_DATA ( thiz, custom_data_field_id, NULL);
	GST_DEBUG ("Done finalizing");
}

/* Set pipeline to PLAYING state */

static void gst_native_play (JNIEnv* env, jobject thiz)
{
	CustomData *data = GET_CUSTOM_DATA ( thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Setting state to PLAYING");
	gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
}

/* Set pipeline to PAUSED state */
static void gst_native_pause (JNIEnv* env, jobject thiz)
{
	CustomData *data = GET_CUSTOM_DATA ( thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Setting state to PAUSED (PLAYING)");
	gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
	//gst_element_set_state (data->pipeline, GST_STATE_PAUSED);
}

/* Static class initializer: retrieve method and field IDs */
static jboolean gst_native_class_init (JNIEnv* env, jclass klass)
{
	custom_data_field_id = env->GetFieldID ( klass, "native_custom_data", "J");
	set_message_method_id = env->GetMethodID ( klass, "setMessage", "(Ljava/lang/String;)V");
	on_gstreamer_initialized_method_id = env->GetMethodID ( klass, "onGStreamerInitialized", "()V");

	on_new_frame_method_id = env->GetMethodID ( klass, "processFrame", "([BI)V");

	if (!custom_data_field_id || !set_message_method_id || !on_gstreamer_initialized_method_id)
	{
		/* We emit this message through the Android log instead of the GStreamer log because the later
		 * has not been initialized yet.
		 */
		__android_log_print (ANDROID_LOG_ERROR, "Streamer", "The calling class does not implement all necessary interface methods");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

static void gst_native_surface_init (JNIEnv *env, jobject thiz, jobject surface)
{
	CustomData *data = GET_CUSTOM_DATA ( thiz, custom_data_field_id);
	if (!data) return;

	if(data->enable_video_sink)
	{
		ANativeWindow *new_native_window = ANativeWindow_fromSurface(env, surface);
		GST_DEBUG ("Received surface %p (native window %p)", surface, new_native_window);

		if (data->native_window) {
			ANativeWindow_release (data->native_window);
			if (data->native_window == new_native_window)
			{
				GST_DEBUG ("New native window is the same as the previous one %p", data->native_window);
				if (data->video_sink)
				{
					gst_video_overlay_expose(GST_VIDEO_OVERLAY(data->video_sink));
					gst_video_overlay_expose(GST_VIDEO_OVERLAY(data->video_sink));
				}
				return;
			} else
			{
				GST_DEBUG ("Released previous native window %p", data->native_window);
				data->initialized = FALSE;
			}
		}
		data->native_window = new_native_window;
	}

	check_initialization_complete (data);
}

static void gst_native_surface_finalize (JNIEnv *env, jobject thiz)
{
	CustomData *data = GET_CUSTOM_DATA (thiz, custom_data_field_id);

	if (!data) return;

	if(data->enable_video_sink)
	{
		GST_DEBUG ("Releasing Native Window %p", data->native_window);

		if (data->video_sink)
		{
			gst_video_overlay_set_window_handle (GST_VIDEO_OVERLAY (data->video_sink), (guintptr)NULL);
			gst_element_set_state (data->pipeline, GST_STATE_READY);
		}

		ANativeWindow_release (data->native_window);
	}
	data->native_window = NULL;
	data->initialized = FALSE;
}

/* List of implemented native methods */
static JNINativeMethod native_methods[] =
{
		{ "nativeInit", "(Ljava/lang/String;IZZ)Z", (void *) gst_native_init},
		{ "nativeFinalize", "()V", (void *) gst_native_finalize},
		{ "nativePlay", "()V", (void *) gst_native_play},
		{ "nativePause", "()V", (void *) gst_native_pause},
		{ "nativeSurfaceInit", "(Ljava/lang/Object;)V", (void *) gst_native_surface_init},
		{ "nativeSurfaceFinalize", "()V", (void *) gst_native_surface_finalize},
		{ "nativeClassInit", "()Z", (void *) gst_native_class_init}
};

/* Library initializer */
jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env = NULL;

	java_vm = vm;

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
		__android_log_print (ANDROID_LOG_ERROR, "StreamHandler", "Could not retrieve JNIEnv");
		return 0;
	}

	jclass klass = env->FindClass ("atlflight/dronecontroller/StreamHandler");
	env->RegisterNatives ( klass, native_methods, G_N_ELEMENTS(native_methods));

	pthread_key_create (&current_jni_env, detach_current_thread);

	return JNI_VERSION_1_4;
}

