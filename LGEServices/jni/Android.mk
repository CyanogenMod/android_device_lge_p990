LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_lge_bridge_BridgeService.cpp \

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libutils \
    libbinder \
    libcutils

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= libbridge_jni

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
