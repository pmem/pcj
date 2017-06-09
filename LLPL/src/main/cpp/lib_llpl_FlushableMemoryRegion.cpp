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

#include "lib_llpl_FlushableMemoryRegion.h"
#include "persistent_heap.h"

JNIEXPORT void JNICALL Java_lib_llpl_FlushableMemoryRegion_nativeFlush
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong size)
{
    pmemobj_persist(pool, (void*)((uint64_t)offset), (size_t)size);
}

JNIEXPORT void JNICALL Java_lib_llpl_FlushableMemoryRegion_nativePutBits
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong value, jint size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    void* dest = (void*)((uint64_t)pmemobj_direct(oid)+(uint64_t)offset);
    void* src = &value;

    memcpy(dest, src, size);
}

JNIEXPORT jint JNICALL Java_lib_llpl_FlushableMemoryRegion_nativeSetFlushed
  (JNIEnv *env, jobject obj, jlong region_offset, jint value)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    void* dest = pmemobj_direct(oid);
    void* src = &value;

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, sizeof(int));
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}

JNIEXPORT jlong JNICALL Java_lib_llpl_FlushableMemoryRegion_nativeGetVolatileAddress
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    return (jlong)(pmemobj_direct(oid));
}
