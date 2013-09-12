LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# For scrypt.
LOCAL_CFLAGS    := -DHAVE_CONFIG_H
LOCAL_MODULE    := nativecrypto

# Can only use SSE on x86.
LOCAL_SRC_FILES := nativecrypto.c \
                   scrypt/c/crypto_scrypt-nosse.c \
                   scrypt/c/sha256.c
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
	                  $(LOCAL_PATH)/scrypt/include

include $(BUILD_SHARED_LIBRARY)
