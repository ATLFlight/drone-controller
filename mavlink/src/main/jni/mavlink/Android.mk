LOCAL_PATH := $(call my-dir)

HOST_SED := sed

include $(CLEAR_VARS)

LOCAL_MODULE    := mavlink_native
LOCAL_SRC_FILES := mavlink_native.c
LOCAL_LDLIBS := -llog -landroid
include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)

include $(BUILD_PACKAGE)
