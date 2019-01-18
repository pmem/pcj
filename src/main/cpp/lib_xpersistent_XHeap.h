/* Copyright (C) 2017-2018  Intel Corporation
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

#include <jni.h>

#ifndef _Included_lib_xpersistent_XHeap
#define _Included_lib_xpersistent_XHeap
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeOpenHeap
  (JNIEnv *, jobject, jstring, jlong);

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeCloseHeap
  (JNIEnv *, jobject);

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocate
  (JNIEnv *, jobject, jlong);

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocateAtomic
  (JNIEnv *, jobject, jlong);

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocateObject
  (JNIEnv *, jobject, jlong);

JNIEXPORT jint JNICALL Java_lib_xpersistent_XHeap_nativeFree
  (JNIEnv *, jobject, jlong);

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeMemoryRegionMemcpy
  (JNIEnv *, jobject, jlong, jlong, jlong, jlong, jlong);

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeToByteArrayMemcpy
  (JNIEnv *, jobject, jlong, jlong, jbyteArray, jint, jint);

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeFromByteArrayMemcpy
  (JNIEnv *, jobject, jbyteArray, jint, jlong, jlong, jint);

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeDebugPool
  (JNIEnv *, jobject, jboolean);

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeCopyBytesToAddress
  (JNIEnv *, jobject, jbyteArray, jint, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
