# Android ndk makefile for cpudetect

LOCAL_PATH := $(call my-dir)

MY_LOCAL_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)


LOCAL_MODULE    		:= libcpudetect
LOCAL_SRC_FILES			:= cpu.cpp
LOCAL_ARM_MODE 			:= thumb
LOCAL_ARM_NEON 			:= false
LOCAL_CFLAGS			:= -fexceptions
LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)