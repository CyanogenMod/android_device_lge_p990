#!/bin/sh

mkdir -p ../../../vendor/lge/p990/proprietary

DIRS="
app
bin
etc/wl
etc/flex
lib/egl
lib/hw
lib/modules
"

for DIR in $DIRS; do
	mkdir -p ../../../vendor/lge/p990/proprietary/$DIR
done

FILES="
etc/flex/flex.db
etc/flex/flex.xml

etc/wl/nvram.txt 
etc/wl/rtecdc-apsta.bin 
etc/wl/rtecdc-mfgtest.bin 
etc/wl/rtecdc.bin 

lib/modules/wireless.ko


lib/hw/gralloc.tegra.so
lib/hw/lights.tegra.so
lib/hw/overlay.tegra.so
lib/hw/sensors.tegra.so
lib/egl/libEGL_tegra.so
lib/egl/libGLESv1_CM_tegra.so
lib/egl/libGLESv2_tegra.so

bin/motion
bin/nvrm_daemon
bin/secureclockd
bin/nvrm_avp.axf
bin/nvmm_wmaprodec.axf
bin/nvmm_wmadec.axf
bin/nvmm_wavdec.axf
bin/nvmm_vc1dec.axf
bin/nvmm_sw_mp3dec.axf
bin/nvmm_sorensondec.axf
bin/nvmm_service.axf
bin/nvmm_reference.axf
bin/nvmm_mpeg4dec.axf
bin/nvmm_mp3dec.axf
bin/nvmm_mp2dec.axf
bin/nvmm_manager.axf
bin/nvmm_jpegenc.axf
bin/nvmm_jpegdec.axf
bin/nvmm_h264dec.axf
bin/nvmm_audiomixer.axf
bin/nvmm_adtsdec.axf
bin/nvmm_aacdec.axf
bin/nvddk_audiofx_transport.axf
bin/nvddk_audiofx_core.axf
bin/lgospd_hid
bin/lgospd
bin/lgdrmserver
bin/immvibed
bin/hdmid
bin/btld
bin/bridgeutil
bin/BCM4329B1_002.002.023.0735.0745.hcd

lib/libnvodm_imager.so
lib/libnvddk_2d_v2.so
lib/libnvddk_2d.so
lib/libnvmm_utils.so
lib/libnvos.so
lib/libnvrm.so
lib/libnvrm_graphics.so
lib/libnvsm.so
lib/libnvmm.so
lib/libnvmm_camera.so
lib/libcamera.so

lib/libril.so
lib/libbridge.so
lib/lge-ril.so

lib/libcgdrv.so
lib/liblgdrm.so
lib/libnvmm_audio.so
lib/libnvmm_contentpipe.so
lib/libnvmm_image.so
lib/libnvmm_manager.so
lib/libnvmm_misc.so
lib/libnvmm_parser.so
lib/libnvmm_tracklist.so
lib/libnvmm_videorenderer.so
lib/libnvmm_video.so
lib/libnvmm_vp6_video.so
lib/libnvmm_writer.so
lib/libnvmm_service.so
lib/libsecureclock.so
lib/libnvomx.so

lib/libnvdispmgr_d.so
lib/liblvvil.so
lib/libnvddk_audiofx.so
lib/libnvodm_query.so
lib/libnvodm_misc.so
lib/libnvrm_channel.so
lib/libnvdispatch_helper.so
lib/libaudio.so
lib/libaudiopolicy.so

etc/pvnvomx.cfg
lib/libpvnvomx.so
"

for FILE in $FILES; do
	adb pull system/$FILE ../../../vendor/lge/p990/proprietary/$FILE
done

