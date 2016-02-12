# Android ndk makefile for nds4droid

APP_STL := gnustl_static
APP_ABI := armeabi armeabi-v7a x86

# For releases
APP_CFLAGS := -Ofast -ftree-vectorize -fsingle-precision-constant -fprefetch-loop-arrays -fvariable-expansion-in-unroller -ffast-math -funroll-loops -fomit-frame-pointer -fno-math-errno -funsafe-math-optimizations -ffinite-math-only -fdata-sections -fbranch-target-load-optimize2 -fno-exceptions -fno-stack-protector -fforce-addr -funswitch-loops -ftree-loop-im -ftree-loop-ivcanon -fivopts -ftree-loop-if-convert-stores -ftree-loop-distribution -floop-interchange -ftree-loop-linear -floop-block -Wno-psabi

# For profiling
#APP_CFLAGS := -Ofast -ftree-vectorize -fsingle-precision-constant -fprefetch-loop-arrays -fvariable-expansion-in-unroller -ffast-math -funroll-loops -fno-math-errno -funsafe-math-optimizations -ffinite-math-only -fdata-sections -fbranch-target-load-optimize2 -fno-exceptions -fno-stack-protector -fforce-addr -funswitch-loops -ftree-loop-im -ftree-loop-ivcanon -fivopts -ftree-loop-if-convert-stores -ftree-loop-distribution -floop-interchange -ftree-loop-linear -floop-block -Wno-psabi

# To generate data for profile guided optimizations
#APP_CFLAGS += -fprofile-generate=/sdcard/profile
#APP_LDFLAGS := -fprofile-generate=/sdcard/profile

# To use profile guide optimizaions
#APP_CFLAGS += -Wno-coverage-mismatch -fprofile-correction -fprofile-use=d:/ds/profile
#APP_LDFLAGS := -fprofile-use=d:/ds/profile

# For debugging
#APP_CFLAGS := -Wno-psabi

NDK_TOOLCHAIN_VERSION=4.9
APP_PLATFORM := android-9
