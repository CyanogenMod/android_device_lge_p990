LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := SecureClock.cpp ISecureClockService.cpp

LOCAL_C_INCLUDES := $(call include-path-for, libharware_legacy)/hardware_legacy $(LOCAL_PATH)/../../include/ $(LOCAL_PATH)/../../include/bridge/

LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder libhardware_legacy libbridge

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libsecureclock

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
