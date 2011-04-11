LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := Bridge.cpp IBridgeService.cpp

LOCAL_C_INCLUDES := $(call include-path-for, libharware_legacy)/hardware_legacy  $(LOCAL_PATH)/../../include/

LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder libhardware_legacy

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libbridge

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
