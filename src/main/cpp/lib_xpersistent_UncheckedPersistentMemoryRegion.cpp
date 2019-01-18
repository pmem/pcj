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

#include "lib_xpersistent_UncheckedPersistentMemoryRegion.h"
#include "persistent_heap.h"
#include "util.h"

// the four put calls below are always called from within a transaction 

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutByte
  (JNIEnv *env, jobject obj, jlong address, jbyte value)
{
    char *ptr = (char*)address;
    int result = pmemobj_tx_add_range_direct(ptr, 1);
    if (result == 0) *ptr = value;
    else throw_persistence_exception(env, "Failed to write byte value.");
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutShort
  (JNIEnv *env, jobject obj, jlong address, jshort value)
{
    int16_t *ptr = (int16_t*)address;
    int result = pmemobj_tx_add_range_direct(ptr, 2);
    if (result == 0) *ptr = value;
    else throw_persistence_exception(env, "Failed to write short value.");
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutInt
  (JNIEnv *env, jobject obj, jlong address, jint value)
{
    int *ptr = (int*)address;
    int result = pmemobj_tx_add_range_direct(ptr, 4);
    if (result == 0) *ptr = value;
    else throw_persistence_exception(env, "Failed to write int value.");
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutLong
  (JNIEnv *env, jobject obj, jlong address, jlong value)
{
    long *ptr = (long*)address;
    int result = pmemobj_tx_add_range_direct(ptr, 8);
    if (result == 0) *ptr = value;
    else throw_persistence_exception(env, "Failed to write long value.");
}


JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutDurableByte
  (JNIEnv *env, jobject obj, jlong address, jbyte value)
{
    char *ptr = (char*)address;
    *ptr = value;
    pmem_persist(ptr, 1);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutDurableShort
  (JNIEnv *env, jobject obj, jlong address, jshort value)
{
    int16_t *ptr = (int16_t*)address;
    *ptr = value;
    pmem_persist(ptr, 2);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutDurableInt
  (JNIEnv *env, jobject obj, jlong address, jint value)
{
    int *ptr = (int*)address;
    *ptr = value;
    pmem_persist(ptr, 4);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutDurableLong
  (JNIEnv *env, jobject obj, jlong address, jlong value)
{
    long *ptr = (long*)address;
    *ptr = value;
    pmem_persist(ptr, 8);
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_getDirectAddress
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)region_offset};
    return (long)pmemobj_direct(oid);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativeFlush
  (JNIEnv *env, jobject obj, jlong address, jlong size)
{
    pmem_persist((const void*)address, size);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_addToTransaction
  (JNIEnv *env, jobject obj, jlong address, jlong size)
{
    pmemobj_tx_add_range_direct((const void *)address, (size_t)size);
}
