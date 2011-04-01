#!/sbin/sh

sleep 3
## Clear "boot-recovery...enable-wipe" flag
dd if=/dev/zero of=/dev/block/mmcblk0p3 seek=6144 count=75 bs=1
