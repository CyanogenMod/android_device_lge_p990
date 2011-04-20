$(call inherit-product, device/lge/star-common/star.mk)

ifeq ($(TARGET_PREBUILT_KERNEL),)
	LOCAL_KERNEL := device/lge/p990/kernel
else
	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

# Board-specific init
PRODUCT_COPY_FILES += \
    device/lge/p990/init.p990.rc:root/init.rc

PRODUCT_COPY_FILES += \
    device/lge/p990/vold.fstab:system/etc/vold.fstab \

PRODUCT_COPY_FILES += \
    $(LOCAL_KERNEL):kernel

PRODUCT_NAME := p990
PRODUCT_DEVICE := p990
PRODUCT_MODEL := LG Optimus 2X
