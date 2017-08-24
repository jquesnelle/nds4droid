# Android ndk makefile for ds4droid

LOCAL_PATH := $(call my-dir)

MY_LOCAL_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    		:= 	libasmjit

LOCAL_SRC_FILES			:=  core/assembler.cpp \
                            core/assert.cpp \
                            core/buffer.cpp \
                            core/compiler.cpp \
                            core/compilercontext.cpp \
                            core/compilerfunc.cpp \
                            core/compileritem.cpp \
                            core/context.cpp \
                            core/cpuinfo.cpp \
                            core/defs.cpp \
                            core/func.cpp \
                            core/logger.cpp \
                            core/memorymanager.cpp \
                            core/memorymarker.cpp \
                            core/operand.cpp \
                            core/stringbuilder.cpp \
                            core/stringutil.cpp \
                            core/virtualmemory.cpp \
                            core/zonememory.cpp \
                            x86/x86assembler.cpp \
                            x86/x86compiler.cpp \
                            x86/x86compilercontext.cpp \
                            x86/x86compilerfunc.cpp \
                            x86/x86compileritem.cpp \
                            x86/x86cpuinfo.cpp \
                            x86/x86defs.cpp \
                            x86/x86func.cpp \
                            x86/x86operand.cpp \
                            x86/x86util.cpp

LOCAL_ARM_NEON 			:= false
LOCAL_CFLAGS			:= -DCOMPRESS_MT

include $(BUILD_STATIC_LIBRARY)