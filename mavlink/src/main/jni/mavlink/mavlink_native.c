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

#include "mavlink_native.h"

#include "common/mavlink.h"
#include <jni.h>
#include <android/log.h>

#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <time.h>
/* Linux / MacOS POSIX timer headers */
#include <sys/time.h>
#include <time.h>
#include <arpa/inet.h>

#define LOGV(TAG,...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(TAG,...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(TAG,...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(TAG,...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(TAG,...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)


#define BUFFER_LENGTH 2041


JNIEXPORT jbyteArray JNICALL Java_atlflight_MavlinkWrapper_build_1manual_1control(JNIEnv *env, jobject thiz, jbyte target, jshort x, jshort y, jshort z, jshort r, jshort buttons) {
    mavlink_message_t msg;
    uint8_t buf[BUFFER_LENGTH];

    uint16_t len = mavlink_msg_manual_control_pack(255, 190, &msg, target, x, y, z, r, buttons);
    // populates buf with msg contents
    len = mavlink_msg_to_send_buffer(buf, &msg);

    jbyteArray manual_control = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, manual_control, 0, len, buf);

    /*
    int i;
    char* buf_str = (char*) malloc (2*len + 1);
    for (i = 0; i < len; i++)
    {
        LOGE("JNI", "jni_code: %02X", buf[i]);
    }*/
    return manual_control;
}

JNIEXPORT jbyteArray JNICALL Java_atlflight_MavlinkWrapper_build_1command_1long(JNIEnv *env,
                jobject thiz, jshort command, jbyte confirmation, jfloat param1, jfloat param2, jfloat param3, jfloat param4, jfloat param5, jfloat param6, jfloat param7) {
    mavlink_message_t msg;
    uint8_t buf[BUFFER_LENGTH];


    uint16_t len = mavlink_msg_command_long_pack(255, 190, &msg, 1,
                                                0, command, confirmation,
                                                param1, param2, param3, param4, param5, param6, param7);
    // populates buf with msg contents
    len = mavlink_msg_to_send_buffer(buf, &msg);

    jbyteArray command_long = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, command_long, 0, len, buf);

    /*
    int i;
    char* buf_str = (char*) malloc (2*len + 1);
    for (i = 0; i < len; i++)
    {
        LOGE("JNI", "jni_code: %02X", buf[i]);
    }*/
    return command_long;
}

