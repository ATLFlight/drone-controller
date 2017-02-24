LOCAL_PATH := $(call my-dir)

ifeq (no-$(GSTREAMER_ROOT),no-null)
$(error GSTREAMER_ROOT is not defined!)
endif

HOST_SED := sed

include $(CLEAR_VARS)

LOCAL_MODULE    := streamhandler
LOCAL_SRC_FILES := StreamHandler.cpp FrameHandler.cpp
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog -landroid
include $(BUILD_SHARED_LIBRARY)

GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/

include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS         := $(GSTREAMER_PLUGINS_CORE) $(GSTREAMER_PLUGINS_SYS) $(GSTREAMER_PLUGINS_EFFECTS)  $(GSTREAMER_PLUGINS_NET) $(GSTREAMER_PLUGINS_CODECS) $(GSTREAMER_PLUGINS_PLAYBACK)
#GSTREAMER_EXTRA_DEPS      := gstreamer-interfaces-0.10 gstreamer-video-0.10
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk

$(call import-module,android/native_app_glue)

include $(BUILD_PACKAGE)
