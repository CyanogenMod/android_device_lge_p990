## Specify phone tech before including full_phone
$(call inherit-product, vendor/cm/config/gsm.mk)

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/lge/p990/p990.mk)

PRODUCT_NAME := cm_p990

# Release name and versioning
PRODUCT_RELEASE_NAME := Optimus2X
PRODUCT_VERSION_DEVICE_SPECIFIC :=

## Device identifier. This must come after all inclusions
PRODUCT_DEVICE := p990

PRODUCT_BUILD_PROP_OVERRIDES += PRODUCT_NAME=lge_star BUILD_ID=GRJ22 BUILD_FINGERPRINT=lge/lge_star/p990_EUR-xx:2.3.4/GRJ22/lgp990-V20l.422C563E:user/release-keys PRIVATE_BUILD_DESC="lge_star-user 2.3.4 GRJ22 422C563E release-keys"
