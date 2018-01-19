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

#include "lib_llpl_AbstractMemoryRegion.h"
#include "persistent_heap.h"

JNIEXPORT jlong JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeGetBits
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jint size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};

    jlong ret = 0;
    char* valChar;
    short* valShort;
    int* valInt;
    long* valLong;
    void* src = (void*)((uint64_t)pmemobj_direct(oid)+(uint64_t)offset);
    switch(size) {
        case 1:
            valChar = (char*)(src);
            ret = *valChar;
            break;
        case 2:
            valShort = (short*)(src);
            ret = *valShort;
            break;
        case 4:
            valInt = (int*)(src);
            ret = *valInt;
            break;
        case 8:
            valLong = (long*)(src);
            ret = *valLong;
            break;
        default:
            printf("Asked to get a bad size: %d\n", size);
            exit(-1);
    }

    //printf("Getting long from region at %lu at offset %lu, address %p, returning value %lu\n", region_offset, offset, src, ret);
    //fflush(stdout);
    return ret;
}

JNIEXPORT jlong JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeGetDirectAddress
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)region_offset};
    return (long)pmemobj_direct(oid);
}

JNIEXPORT jint JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeMemoryRegionMemcpyRaw
  (JNIEnv *env, jobject obj, jlong src_region, jlong src_offset, jlong dest_region, jlong dest_offset, jlong length)
{
    PMEMoid src_oid = {get_uuid_lo(), (uint64_t)src_region};
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};

    void* src = (void*)((uint64_t)pmemobj_direct(src_oid)+(uint64_t)src_offset);
    void* dest = (void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset);

    memcpy(dest, src, (uint64_t)length);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeFromByteArrayMemcpyRaw
  (JNIEnv *env, jobject obj, jbyteArray src_array, jint src_offset, jlong dest_region, jlong dest_offset, jint length)
{
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};
    jbyte* dest = (jbyte*)((void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset));

    jboolean is_copy;
    jbyte* bytes = env->GetByteArrayElements(src_array, &is_copy);

    memcpy((void*)dest, (void*)(bytes+src_offset), length);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeMemoryRegionMemsetRaw
  (JNIEnv *env, jobject obj, jlong region, jlong offset, jint val, jlong length)
{
    PMEMoid region_oid = {get_uuid_lo(), (uint64_t)region};
    void* dest = (void*)((uint64_t)pmemobj_direct(region_oid)+(uint64_t)offset);
    memset(dest, val, (size_t)length);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_llpl_AbstractMemoryRegion_nativeSetSize
  (JNIEnv *env, jobject obj, jlong region, jlong offset, jlong size)
{
    PMEMoid oid = {get_uuid_lo(), region};
    void* dest = (void*)((uint64_t)pmemobj_direct(oid)+(uint64_t)offset);
    void* src = &size;

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, 8);
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}