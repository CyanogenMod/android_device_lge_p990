# The gps config appropriate for this device
$(call inherit-product, device/common/gps/gps_us_supl.mk)

$(call inherit-product-if-exists, vendor/lge/p990/p990-vendor.mk)

DEVICE_PACKAGE_OVERLAYS += device/lge/p990/overlay


ifeq ($(TARGET_PREBUILT_KERNEL),)
	LOCAL_KERNEL := device/lge/p990/kernel
else
	LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

# Live wallpaper packages
PRODUCT_PACKAGES += \
    LiveWallpapers \
    LiveWallpapersPicker \
    MagicSmokeWallpapers \
    VisualizationWallpapers \
    librs_jni

# Publish that we support the live wallpaper feature.
PRODUCT_COPY_FILES += \
    packages/wallpapers/LivePicker/android.software.live_wallpaper.xml:/system/etc/permissions/android.software.live_wallpaper.xml

# Board-specific init
PRODUCT_COPY_FILES += \
    device/lge/p990/ueventd.tegra.rc:root/ueventd.tegra.rc \
    device/lge/p990/init.p990.rc:root/init.rc

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/recovery/postrecoveryboot.sh:recovery/root/sbin/postrecoveryboot.sh

PRODUCT_COPY_FILES += \
    $(LOCAL_KERNEL):kernel

##HAL
PRODUCT_COPY_FILES += \
    vendor/lge/p990/proprietary/lib/hw/gralloc.tegra.so:system/lib/hw/gralloc.tegra.so \
    vendor/lge/p990/proprietary/lib/hw/lights.tegra.so:system/lib/hw/lights.tegra.so \
    vendor/lge/p990/proprietary/lib/hw/overlay.tegra.so:system/lib/hw/overlay.tegra.so \
    vendor/lge/p990/proprietary/lib/hw/sensors.tegra.so:system/lib/hw/sensors.tegra.so

##EGL
PRODUCT_COPY_FILES += \
    vendor/lge/p990/proprietary/lib/egl/libEGL_tegra.so:system/lib/egl/libEGL_tegra.so \
    vendor/lge/p990/proprietary/lib/egl/libGLESv1_CM_tegra.so:system/lib/egl/libGLESv1_CM_tegra.so \
    vendor/lge/p990/proprietary/lib/egl/libGLESv2_tegra.so:system/lib/egl/libGLESv2_tegra.so \
    device/lge/p990/egl.cfg:system/lib/egl/egl.cfg

##Wifi
PRODUCT_COPY_FILES += \
    device/lge/p990/wpa_supplicant.conf:system/etc/wifi/wpa_supplicant.conf \
    device/lge/p990/dhcpcd.conf:system/etc/dhcpcd/dhcpcd.conf \
    vendor/lge/p990/proprietary/etc/wl/nvram.txt:system/etc/wl/nvram.txt \
    vendor/lge/p990/proprietary/etc/wl/rtecdc-apsta.bin:system/etc/wl/rtecdc-apsta.bin \
    vendor/lge/p990/proprietary/etc/wl/rtecdc-mfgtest.bin:system/etc/wl/rtecdc-mfgtest.bin \
    vendor/lge/p990/proprietary/etc/wl/rtecdc.bin:system/etc/wl/rtecdc.bin \
    device/lge/p990/prebuilt/wireless.ko:system/lib/modules/wireless.ko

##GPS
PRODUCT_COPY_FILES += \
    device/lge/p990/gps_brcm_conf.xml:system/etc/gps_brcm_conf.xml \
    vendor/lge/p990/proprietary/bin/glgps:system/bin/glgps \
    vendor/lge/p990/proprietary/lib/libgps.so:obj/lib/libgps.so \
    vendor/lge/p990/proprietary/lib/libgps.so:system/lib/libgps.so

## Hardware capabilities
PRODUCT_COPY_FILES += \
    frameworks/base/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml \
    frameworks/base/data/etc/android.hardware.camera.flash-autofocus.xml:system/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/base/data/etc/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
    frameworks/base/data/etc/android.hardware.telephony.gsm.xml:system/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/base/data/etc/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    frameworks/base/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/base/data/etc/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/base/data/etc/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    frameworks/base/data/etc/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml \
    frameworks/base/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml

PRODUCT_COPY_FILES += \
    device/lge/p990/vold.fstab:system/etc/vold.fstab

## OMX
PRODUCT_COPY_FILES += \
    vendor/lge/p990/proprietary/etc/pvnvomx.cfg:system/etc/pvnvomx.cfg \
    vendor/lge/p990/proprietary/lib/libpvnvomx.so:system/lib/libpvnvomx.so

PRODUCT_COPY_FILES += \
    vendor/lge/p990/proprietary/etc/flex/flex.db:system/etc/flex/flex.db \
    vendor/lge/p990/proprietary/etc/flex/flex.xml:system/etc/flex/flex.xml \
    vendor/lge/p990/proprietary/bin/BCM4329B1_002.002.023.0735.0745.hcd:system/etc/firmware/BCM4329B1_002.002.023.0735.0745.hcd \
    vendor/lge/p990/proprietary/bin/motion:system/bin/motion \
    vendor/lge/p990/proprietary/bin/nvrm_daemon:system/bin/nvrm_daemon \
    vendor/lge/p990/proprietary/bin/nvrm_avp.axf:system/bin/nvrm_avp.axf \
    vendor/lge/p990/proprietary/bin/nvddk_audiofx_core.axf:system/bin/nvddk_audiofx_core.axf \
    vendor/lge/p990/proprietary/bin/secureclockd:system/bin/secureclockd \
    vendor/lge/p990/proprietary/bin/nvrm_avp.axf:system/bin/nvrm_avp.axf \
    vendor/lge/p990/proprietary/bin/nvmm_wmaprodec.axf:system/bin/nvmm_wmaprodec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_wmadec.axf:system/bin/nvmm_wmadec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_wavdec.axf:system/bin/nvmm_wavdec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_vc1dec.axf:system/bin/nvmm_vc1dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_sw_mp3dec.axf:system/bin/nvmm_sw_mp3dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_sorensondec.axf:system/bin/nvmm_sorensondec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_service.axf:system/bin/nvmm_service.axf \
    vendor/lge/p990/proprietary/bin/nvmm_reference.axf:system/bin/nvmm_reference.axf \
    vendor/lge/p990/proprietary/bin/nvmm_mpeg4dec.axf:system/bin/nvmm_mpeg4dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_mp3dec.axf:system/bin/nvmm_mp3dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_mp2dec.axf:system/bin/nvmm_mp2dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_manager.axf:system/bin/nvmm_manager.axf \
    vendor/lge/p990/proprietary/bin/nvmm_jpegenc.axf:system/bin/nvmm_jpegenc.axf \
    vendor/lge/p990/proprietary/bin/nvmm_jpegdec.axf:system/bin/nvmm_jpegdec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_h264dec.axf:system/bin/nvmm_h264dec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_audiomixer.axf:system/bin/nvmm_audiomixer.axf \
    vendor/lge/p990/proprietary/bin/nvmm_adtsdec.axf:system/bin/nvmm_adtsdec.axf \
    vendor/lge/p990/proprietary/bin/nvmm_aacdec.axf:system/bin/nvmm_aacdec.axf \
    vendor/lge/p990/proprietary/bin/nvddk_audiofx_transport.axf:system/bin/nvddk_audiofx_transport.axf \
    vendor/lge/p990/proprietary/bin/nvddk_audiofx_core.axf:system/bin/nvddk_audiofx_core.axf \
    vendor/lge/p990/proprietary/bin/lgospd_hid:system/bin/lgospd_hid \
    vendor/lge/p990/proprietary/bin/lgospd:system/bin/lgospd \
    vendor/lge/p990/proprietary/bin/lgdrmserver:system/bin/lgdrmserver \
    vendor/lge/p990/proprietary/bin/immvibed:system/bin/immvibed \
    vendor/lge/p990/proprietary/bin/hdmid:system/bin/hdmid \
    vendor/lge/p990/proprietary/bin/btld:system/bin/btld \
    vendor/lge/p990/proprietary/bin/bridgeutil:system/bin/bridgeutil \
    device/lge/p990/init.vsnet:system/bin/init.vsnet \
    vendor/lge/p990/proprietary/lib/libril.so:system/lib/libril.so \
    vendor/lge/p990/proprietary/lib/libnvos.so:system/lib/libnvos.so \
    vendor/lge/p990/proprietary/lib/libnvrm.so:system/lib/libnvrm.so \
    vendor/lge/p990/proprietary/lib/libbridge.so:system/lib/libbridge.so \
    vendor/lge/p990/proprietary/lib/lge-ril.so:system/lib/lge-ril.so \
    vendor/lge/p990/proprietary/lib/libnvmm_camera.so:system/lib/libnvmm_camera.so \
    vendor/lge/p990/proprietary/lib/libcamera.so:obj/lib/libcamera.so \
    vendor/lge/p990/proprietary/lib/libcamera.so:system/lib/libcamera.so \
    vendor/lge/p990/proprietary/lib/libnvddk_2d_v2.so:system/lib/libnvddk_2d_v2.so \
    vendor/lge/p990/proprietary/lib/libnvodm_imager.so:system/lib/libnvodm_imager.so \
    vendor/lge/p990/proprietary/lib/libnvrm_graphics.so:system/lib/libnvrm_graphics.so \
    vendor/lge/p990/proprietary/lib/libnvddk_2d.so:system/lib/libnvddk_2d.so \
    vendor/lge/p990/proprietary/lib/libnvsm.so:system/lib/libnvsm.so \
    vendor/lge/p990/proprietary/lib/libnvmm_utils.so:system/lib/libnvmm_utils.so \
    vendor/lge/p990/proprietary/lib/libnvmm.so:system/lib/libnvmm.so \
    vendor/lge/p990/proprietary/lib/libnvdispmgr_d.so:system/lib/libnvdispmgr_d.so \
    vendor/lge/p990/proprietary/lib/liblvvil.so:system/lib/liblvvil.so \
    vendor/lge/p990/proprietary/lib/libnvddk_audiofx.so:system/lib/libnvddk_audiofx.so \
    vendor/lge/p990/proprietary/lib/libnvodm_query.so:system/lib/libnvodm_query.so \
    vendor/lge/p990/proprietary/lib/libnvodm_misc.so:system/lib/libnvodm_misc.so \
    vendor/lge/p990/proprietary/lib/libnvrm_channel.so:system/lib/libnvrm_channel.so \
    vendor/lge/p990/proprietary/lib/libnvdispatch_helper.so:system/lib/libnvdispatch_helper.so \
    vendor/lge/p990/proprietary/lib/libnvomxilclient.so:system/lib/libnvomxilclient.so \
    vendor/lge/p990/proprietary/lib/libnvmm_logger.so:system/lib/libnvmm_logger.so \
    vendor/lge/p990/proprietary/lib/libcgdrv.so:system/lib/libcgdrv.so \
    vendor/lge/p990/proprietary/lib/liblgdrm.so:system/lib/liblgdrm.so \
    vendor/lge/p990/proprietary/lib/libnvmm_audio.so:system/lib/libnvmm_audio.so \
    vendor/lge/p990/proprietary/lib/libnvmm_contentpipe.so:system/lib/libnvmm_contentpipe.so \
    vendor/lge/p990/proprietary/lib/libnvmm_image.so:system/lib/libnvmm_image.so \
    vendor/lge/p990/proprietary/lib/libnvmm_manager.so:system/lib/libnvmm_manager.so \
    vendor/lge/p990/proprietary/lib/libnvmm_misc.so:system/lib/libnvmm_misc.so \
    vendor/lge/p990/proprietary/lib/libnvmm_parser.so:system/lib/libnvmm_parser.so \
    vendor/lge/p990/proprietary/lib/libnvmm_tracklist.so:system/lib/libnvmm_tracklist.so \
    vendor/lge/p990/proprietary/lib/libnvmm_video.so:system/lib/libnvmm_video.so \
    vendor/lge/p990/proprietary/lib/libnvmm_videorenderer.so:system/lib/libnvmm_videorenderer.so \
    vendor/lge/p990/proprietary/lib/libnvmm_vp6_video.so:system/lib/libnvmm_vp6_video.so \
    vendor/lge/p990/proprietary/lib/libnvmm_writer.so:system/lib/libnvmm_writer.so \
    vendor/lge/p990/proprietary/lib/libnvmm_service.so:system/lib/libnvmm_service.so \
    vendor/lge/p990/proprietary/lib/libsecureclock.so:system/lib/libsecureclock.so \
    vendor/lge/p990/proprietary/lib/libnvomx.so:system/lib/libnvomx.so \
    vendor/lge/p990/proprietary/lib/libnvwsi.so:system/lib/libnvwsi.so \
    vendor/lge/p990/proprietary/lib/libaudio.so:obj/lib/libaudio.so \
    vendor/lge/p990/proprietary/lib/libaudiopolicy.so:obj/lib/libaudiopolicy.so \
    vendor/lge/p990/proprietary/lib/libaudio.so:system/lib/libaudio.so \
    vendor/lge/p990/proprietary/lib/libaudiopolicy.so:system/lib/libaudiopolicy.so

PRODUCT_COPY_FILES += \
    device/lge/p990/prebuilt/rild:system/bin/rild \
    device/lge/p990/prebuilt/setup-recovery:system/bin/setup-recovery

$(call inherit-product, build/target/product/full_base.mk)

PRODUCT_LOCALES += hdpi


PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0
PRODUCT_NAME := p990
PRODUCT_DEVICE := p990
PRODUCT_MODEL := LG Optimus 2X
