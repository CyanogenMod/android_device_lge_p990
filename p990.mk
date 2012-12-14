# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_eu.mk)

$(call inherit-product, device/lge/star-common/star.mk)

DEVICE_PACKAGE_OVERLAYS += device/lge/p990/overlay

# Inherit non-open-source blobs.
$(call inherit-product-if-exists, vendor/lge/p990/p990-vendor.mk)

# Board-specific init
PRODUCT_COPY_FILES += \
    device/lge/p990/init.p990.rc:root/init.star.rc \
    $(LOCAL_PATH)/ueventd.tegra.rc:root/ueventd.star.rc \
    $(LOCAL_PATH)/fstab.p990:root/fstab.p990

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/vold.fstab:system/etc/vold.fstab \
    $(LOCAL_PATH)/init.vsnet:system/bin/init.vsnet \
    $(LOCAL_PATH)/init.vsnet-down:system/bin/init.vsnet-down \
    $(LOCAL_PATH)/gps_brcm_conf.xml:system/etc/gps_brcm_conf.xml \
    $(LOCAL_PATH)/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/prebuilt/wireless.ko:system/lib/modules/wireless.ko

PRODUCT_PACKAGES += \
    lgcpversion

# Filesystem management tools
PRODUCT_PACKAGES += \
	setup_fs

# BlueZ test tools
PRODUCT_PACKAGES += \
	hciconfig \
	hcitool

PRODUCT_PACKAGES += send_bug
PRODUCT_COPY_FILES += \
	system/extras/bugmailer/bugmailer.sh:system/bin/bugmailer.sh \
	system/extras/bugmailer/send_bug:system/bin/send_bug

PRODUCT_NAME := full_p990
PRODUCT_DEVICE := p990
PRODUCT_MODEL := LG-P990
