ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	secureclockd.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../include/ $(LOCAL_PATH)/../../include/bridge/

LOCAL_SHARED_LIBRARIES := libcutils libutils libc libsecureclock


LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= secureclockd

include $(BUILD_EXECUTABLE)

endif
