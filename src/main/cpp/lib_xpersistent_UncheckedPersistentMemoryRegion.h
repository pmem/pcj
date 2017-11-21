/* Copyright (C) 2017  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class lib_xpersistent_UncheckedPersistentMemoryRegion */

#ifndef _Included_lib_xpersistent_UncheckedPersistentMemoryRegion
#define _Included_lib_xpersistent_UncheckedPersistentMemoryRegion
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     lib_xpersistent_UncheckedPersistentMemoryRegion
 * Method:    nativePutLong
 * Signature: (JJJI)V
 */
JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutLong
  (JNIEnv *, jobject, jlong, jlong, jlong, jint);

/*
 * Class:     lib_xpersistent_UncheckedPersistentMemoryRegion
 * Method:    getDirectAddress
 * Signature: (JJJ)J
 */

JNIEXPORT jlong JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_getDirectAddress
  (JNIEnv *env, jobject obj, jlong region_offset);

#ifdef __cplusplus
}
#endif
#endif
